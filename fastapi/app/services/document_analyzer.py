import json
import logging
import re
from typing import Any, Dict, List

from config.settings import settings
from services import anythingllm_client
from services import pgc as pgc_ao

logger = logging.getLogger(__name__)

# Regra de ouro deste ficheiro:
#   A IA (ou as regras) só CLASSIFICA o tipo de documento e EXTRAI valores.
#   As CONTAS do lançamento vêm SEMPRE, de forma determinística, do módulo
#   pgc_ao (Decreto 82/01). O modelo nunca escolhe códigos de conta — assim
#   nunca inventa contas nem viola o plano oficial. Esta parte NUNCA muda
#   com o motor de IA usado (Gemini, Ollama directo, ou agora AnythingLLM).


class DocumentAnalyzer:
    """Classificador de documentos contábeis: consulta o workspace do
    AnythingLLM (RAG + LLM local via Ollama, gerido pelo próprio AnythingLLM),
    com fallback a regras se o AnythingLLM não estiver disponível.

    O caminho anterior (RAG à mão + Ollama directo) fica preservado em
    fastapi/app/legacy/ para comparação na tese, mas já não é usado aqui.
    """

    def __init__(self):
        self.use_ai = anythingllm_client.disponivel()
        if not self.use_ai:
            logger.warning(
                "AnythingLLM indisponível — a usar classificação por regras."
            )
        else:
            logger.info("AnythingLLM configurado com sucesso.")

    # ── Ponto de entrada ─────────────────────────────────────────────────────
    def analyze_document(self, text: str, ocr_data: Dict[str, Any]) -> Dict[str, Any]:
        try:
            if self.use_ai:
                classificacao = self._classificar_com_anythingllm(text, ocr_data)
            else:
                classificacao = self._classificar_com_regras(text, ocr_data)
        except Exception as e:
            logger.error(f"Erro na classificação, a usar regras: {e}")
            classificacao = self._classificar_com_regras(text, ocr_data)

        return self._montar_resposta(classificacao, ocr_data)

    # ── Montagem do lançamento (contas sempre do pgc_ao) ─────────────────────
    def _montar_resposta(self, c: Dict[str, Any], ocr_data: Dict[str, Any]) -> Dict[str, Any]:
        tipo = c.get("tipo", pgc_ao.TIPO_A_CLASSIFICAR)
        if tipo not in pgc_ao.TIPOS_VALIDOS:
            tipo = pgc_ao.TIPO_A_CLASSIFICAR

        valor = c.get("valor", "0")
        descricao = (c.get("descricao") or "").strip()
        # Taxa de IVA referida no próprio documento (14% ou 7%) — se o
        # classificador não a indicar, pgc_ao.construir_lancamento usa a
        # taxa geral (14%) por omissão (ver pgc._resolver_taxa_iva).
        taxa_iva = c.get("taxaIva")

        linhas = pgc_ao.construir_lancamento(tipo, valor, descricao, taxa_iva=taxa_iva)

        # Verificação defensiva: garantir partidas dobradas antes de devolver.
        if not pgc_ao.lancamento_equilibrado(linhas):
            logger.error("Lançamento gerado não está equilibrado — a marcar A_CLASSIFICAR.")
            tipo = pgc_ao.TIPO_A_CLASSIFICAR
            linhas = pgc_ao.construir_lancamento(tipo, valor, descricao)

        return {
            "success": True,
            "tipoDocumento": tipo,
            "descricao": descricao or "Documento contabilístico",
            "valorTotal": f"{pgc_ao._dec(valor):.2f}",
            "moeda": "AOA",
            "entidade": c.get("entidade", ""),
            "nif": c.get("nif", ""),
            "data": c.get("data", ""),
            "confianca": c.get("confianca", 70),
            "modelo": c.get("modelo", "regras"),
            "fundamentacao": c.get("fundamentacao", ""),
            "linhas": linhas,
        }

    # ── Classificação via AnythingLLM: 2 workspaces separados ────────────────
    #   1) EXEMPLOS decide a classificação (tipo/valor/entidade), por
    #      semelhança com documentos de exemplo já classificados.
    #   2) NORMAS só é consultado depois, para fundamentar legalmente o tipo
    #      já decidido — falha aqui não deita fora uma classificação válida.
    def _classificar_com_anythingllm(self, text: str, ocr_data: Dict[str, Any]) -> Dict[str, Any]:
        accounting = ocr_data.get("accounting_data", {})
        texto = (text or "")[:3000]

        prompt = f"""Você é um especialista em contabilidade angolana (PGC-AO, Decreto n.º 82/01).
Classifique o documento e extraia os dados. NÃO indique contas contabilísticas.

TEXTO DO DOCUMENTO:
{texto}

DADOS DO OCR:
- Valores: {accounting.get('valores_monetarios', [])}
- Datas: {accounting.get('datas', [])}
- NIF: {accounting.get('nif', '')}
- Tipo detetado: {accounting.get('tipo_documento', 'desconhecido')}

Responda APENAS com JSON válido, sem texto à volta, neste formato exacto:
{{
  "tipo": "um de: compra_mercadoria, compra_servico, venda_mercadoria, prestacao_servico, pagamento_fornecedor, recebimento_cliente, a_classificar",
  "valor": "valor total numérico, só dígitos e ponto decimal (ex: 150000.00)",
  "descricao": "descrição curta do documento",
  "entidade": "nome da empresa/pessoa, se houver",
  "nif": "NIF se houver",
  "data": "data (dd/mm/aaaa) se houver",
  "taxaIva": "taxa de IVA referida no documento — só \"14\" ou \"7\" — ou vazio se o documento não indicar nenhuma taxa de IVA",
  "confianca": 0
}}"""

        resultado = anythingllm_client.consultar_workspace(prompt, settings.ANYTHINGLLM_WORKSPACE_EXEMPLOS)
        dados = self._extrair_json(resultado["resposta"])

        dados["modelo"] = f"anythingllm:{settings.ANYTHINGLLM_WORKSPACE_EXEMPLOS}+{settings.ANYTHINGLLM_WORKSPACE_NORMAS}"
        dados["fundamentacao"] = self._obter_fundamentacao(dados.get("tipo"), resultado["fontes"])

        logger.info("AnythingLLM (exemplos) classificou como: %s", dados.get("tipo"))
        return dados

    def _obter_fundamentacao(self, tipo: Any, fontes_exemplos: List[str]) -> str:
        """Segunda chamada, ao workspace de normas, só para citar a base
        legal do tipo já decidido. Uma falha aqui é só registada em log —
        não deve invalidar uma classificação já bem-sucedida."""
        if not tipo or tipo == pgc_ao.TIPO_A_CLASSIFICAR:
            return " | ".join(fontes_exemplos)

        try:
            pergunta = (
                f"Qual é o fundamento legal no Decreto n.º 82/01 (PGC-AO) para "
                f"classificar um documento contabilístico como '{tipo}'? "
                f"Responda de forma breve, citando o artigo ou secção relevante."
            )
            resultado = anythingllm_client.consultar_workspace(pergunta, settings.ANYTHINGLLM_WORKSPACE_NORMAS)
            fontes = resultado["fontes"] or ([resultado["resposta"][:200]] if resultado["resposta"] else [])
            return " | ".join(fontes) if fontes else " | ".join(fontes_exemplos)
        except anythingllm_client.AnythingLLMError as e:
            logger.warning("Não foi possível obter fundamentação legal (workspace normas): %s", e)
            return " | ".join(fontes_exemplos)

    def _extrair_json(self, texto: str) -> Dict[str, Any]:
        """O AnythingLLM devolve texto de chat por defeito — mesmo pedindo
        'só JSON', o modelo por vezes 'suja' a resposta com texto à volta,
        por isso este parser é tolerante em vez de um json.loads directo."""
        limpo = texto.strip()
        if "```json" in limpo:
            limpo = limpo.split("```json")[1].split("```")[0]
        elif "```" in limpo:
            limpo = limpo.split("```")[1].split("```")[0]

        limpo = limpo.strip()
        try:
            return json.loads(limpo)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", limpo, re.DOTALL)
            if match:
                return json.loads(match.group(0))
            raise

    # ── Classificação por regras (fallback) ──────────────────────────────────
    def _classificar_com_regras(self, text: str, ocr_data: Dict[str, Any]) -> Dict[str, Any]:
        accounting = ocr_data.get("accounting_data", {})
        t = (text or "").lower()

        valores = accounting.get("valores_monetarios", [])
        valor = valores[0] if valores else "0"
        datas = accounting.get("datas", [])
        data = datas[0] if datas else ""

        recebimento = any(k in t for k in ["recibo de pagamento", "recebido", "recebemos", "entrada de caixa"])
        pagamento = any(k in t for k in ["pagamento a", "pago a fornecedor", "comprovativo de pagamento", "saída de caixa"])
        venda = any(k in t for k in ["fatura de venda", "factura de venda", "venda", "vendemos", "cliente"])
        servico = any(k in t for k in ["serviço", "servico", "prestação", "prestacao", "renda", "aluguer",
                                       "água", "agua", "energia", "electricidade", "eletricidade", "transporte",
                                       "seguro", "telefone", "internet"])
        compra = any(k in t for k in ["fatura", "factura", "compra", "fornecedor", "mercadoria"])

        if recebimento:
            tipo = pgc_ao.TIPO_RECEBIMENTO_CLIENTE
        elif pagamento:
            tipo = pgc_ao.TIPO_PAGAMENTO_FORNECEDOR
        elif venda and servico:
            tipo = pgc_ao.TIPO_PRESTACAO_SERVICO
        elif venda:
            tipo = pgc_ao.TIPO_VENDA_MERCADORIA
        elif servico:
            tipo = pgc_ao.TIPO_COMPRA_SERVICO
        elif compra:
            tipo = pgc_ao.TIPO_COMPRA_MERCADORIA
        else:
            tipo = pgc_ao.TIPO_A_CLASSIFICAR

        return {
            "tipo": tipo,
            "valor": valor,
            "descricao": (text or "")[:150].replace("\n", " ").strip(),
            "entidade": "",
            "nif": accounting.get("nif", "") or "",
            "data": data,
            "taxaIva": self._extrair_taxa_iva(text or ""),
            "confianca": 65 if tipo != pgc_ao.TIPO_A_CLASSIFICAR else 40,
            "modelo": "regras",
            "fundamentacao": "",
        }

    def _extrair_taxa_iva(self, texto: str) -> str:
        """Procura no texto do documento uma menção explícita à taxa de IVA
        (ex: "IVA 14%", "Taxa de IVA: 7%") — só reconhece 14 ou 7, as
        únicas taxas de IVA em vigor em Angola tratadas por este projeto.
        Vazio se não encontrar, para pgc_ao usar a taxa geral por omissão."""
        match = re.search(r"iva[^0-9%]{0,15}(14|7)\s*%", texto, re.IGNORECASE)
        return match.group(1) if match else ""
