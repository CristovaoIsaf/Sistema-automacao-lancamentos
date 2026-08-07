import json
import logging
import re
from typing import Any, Dict, List

from config.settings import settings
from services import anythingllm_client
from services import pgc as pgc_ao

logger = logging.getLogger(__name__)

# Regra de ouro deste ficheiro:
#   A IA (ou as regras) só CLASSIFICA o tipo de documento. Toda a extração
#   de dados (valor, entidade, NIF, data, taxa de IVA) já vem tratada e
#   determinística de services/regex_extract.py (via ocr_service.py) —
#   nunca é pedida à IA. Isto evita que a IA tenha de "pensar" em coisas
#   que já são resolvidas de forma fiável e testável antes de lhe chegar.
#   As CONTAS do lançamento continuam a vir SEMPRE, de forma determinística,
#   do módulo pgc_ao (Decreto 82/01). O modelo nunca escolhe códigos de
#   conta — assim nunca inventa contas nem viola o plano oficial.

TIPOS_COMPRA = {
    pgc_ao.TIPO_COMPRA_MERCADORIA,
    pgc_ao.TIPO_COMPRA_SERVICO,
    pgc_ao.TIPO_PAGAMENTO_FORNECEDOR,
}


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
        dados_fatura = ocr_data.get("dados_fatura", {}) or {}

        try:
            if self.use_ai:
                classificacao = self._classificar_com_anythingllm(text, dados_fatura)
            else:
                classificacao = self._classificar_com_regras(text, dados_fatura)
        except Exception as e:
            logger.error(f"Erro na classificação, a usar regras: {e}")
            classificacao = self._classificar_com_regras(text, dados_fatura)

        return self._montar_resposta(classificacao, dados_fatura)

    # ── Montagem do lançamento (contas sempre do pgc_ao) ─────────────────────
    def _montar_resposta(self, c: Dict[str, Any], dados_fatura: Dict[str, Any]) -> Dict[str, Any]:
        tipo = c.get("tipo", pgc_ao.TIPO_A_CLASSIFICAR)
        if tipo not in pgc_ao.TIPOS_VALIDOS:
            tipo = pgc_ao.TIPO_A_CLASSIFICAR

        valor = dados_fatura.get("valor_total_aoa") or "0"
        descricao = (c.get("descricao") or "").strip()
        taxa_iva = self._taxa_iva_tratada(dados_fatura)

        linhas = pgc_ao.construir_lancamento(tipo, valor, descricao, taxa_iva=taxa_iva)

        # Verificação defensiva: garantir partidas dobradas antes de devolver.
        if not pgc_ao.lancamento_equilibrado(linhas):
            logger.error("Lançamento gerado não está equilibrado — a marcar A_CLASSIFICAR.")
            tipo = pgc_ao.TIPO_A_CLASSIFICAR
            linhas = pgc_ao.construir_lancamento(tipo, valor, descricao)

        entidade, nif = self._entidade_e_nif(tipo, dados_fatura)

        return {
            "success": True,
            "tipoDocumento": tipo,
            "descricao": descricao or "Documento contabilístico",
            "valorTotal": f"{pgc_ao._dec(valor):.2f}",
            "moeda": "AOA",
            "entidade": entidade,
            "nif": nif,
            "data": dados_fatura.get("data_emissao") or "",
            "confianca": c.get("confianca", 70),
            "modelo": c.get("modelo", "regras"),
            "fundamentacao": c.get("fundamentacao", ""),
            "categoria": pgc_ao.categoria_do_tipo(tipo),
            "linhas": linhas,
        }

    def _entidade_e_nif(self, tipo: str, dados_fatura: Dict[str, Any]) -> tuple:
        """Escolhe qual das partes do documento é "a entidade" do lançamento:
        numa compra/pagamento a fornecedor, é o emitente (quem nos vendeu);
        numa venda/prestação/recebimento, é o adquirente (nosso cliente).
        Cai para a outra parte se a preferida não tiver sido extraída."""
        if tipo in TIPOS_COMPRA:
            nome = dados_fatura.get("emitente_nome") or dados_fatura.get("adquirente_nome") or ""
            nif = dados_fatura.get("emitente_nif") or dados_fatura.get("adquirente_nif") or ""
        else:
            nome = dados_fatura.get("adquirente_nome") or dados_fatura.get("emitente_nome") or ""
            nif = dados_fatura.get("adquirente_nif") or dados_fatura.get("emitente_nif") or ""
        return nome, nif

    def _taxa_iva_tratada(self, dados_fatura: Dict[str, Any]) -> str:
        """Só reconhece 14% ou 7% (as taxas de IVA em vigor em Angola
        tratadas por este projeto) entre as taxas já detetadas por regex em
        regex_extract.taxas_iva_encontradas — fonte única, sem re-extrair."""
        for taxa in dados_fatura.get("taxas_iva_encontradas") or []:
            taxa_normalizada = str(taxa).replace(",", ".").split(".")[0]
            if taxa_normalizada in ("14", "7"):
                return taxa_normalizada
        return ""

    # ── Classificação via AnythingLLM: 2 workspaces separados ────────────────
    #   1) EXEMPLOS decide a classificação (tipo), por semelhança com
    #      documentos de exemplo já classificados — usando dados já tratados,
    #      não o texto em bruto como fonte primária.
    #   2) NORMAS só é consultado depois, para fundamentar legalmente o tipo
    #      já decidido — falha aqui não deita fora uma classificação válida.
    def _classificar_com_anythingllm(self, text: str, dados_fatura: Dict[str, Any]) -> Dict[str, Any]:
        texto = (text or "")[:2000]

        prompt = f"""Você é um especialista em contabilidade angolana (PGC-AO, Decreto n.º 82/01).

Os dados abaixo já foram extraídos do documento de forma determinística (regex) — não os reinterprete nem tente corrigi-los. A sua única tarefa é CLASSIFICAR o tipo de operação contabilística deste documento. Não indique contas contabilísticas nem valores — isso já está tratado.

DADOS JÁ EXTRAÍDOS:
- Emitente: {dados_fatura.get('emitente_nome') or '(não identificado)'} (NIF {dados_fatura.get('emitente_nif') or '?'})
- Adquirente: {dados_fatura.get('adquirente_nome') or '(não identificado)'} (NIF {dados_fatura.get('adquirente_nif') or '?'})
- Nº documento: {dados_fatura.get('numero_fatura') or '?'}
- Valor total: {dados_fatura.get('valor_total_aoa') or '?'} AOA
- Data: {dados_fatura.get('data_emissao') or '?'}
- Rótulo do tipo de documento detetado no texto: {dados_fatura.get('tipo_documento') or 'desconhecido'}

EXCERTO DO TEXTO (só para contexto/desambiguação, não para extrair dados novos):
{texto}

Responda APENAS com JSON válido, sem texto à volta, neste formato exacto:
{{
  "tipo": "um de: compra_mercadoria, compra_servico, venda_mercadoria, prestacao_servico, pagamento_fornecedor, recebimento_cliente, a_classificar",
  "descricao": "descrição curta do documento",
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
    # Só classifica o tipo por palavras-chave — valor/entidade/NIF/data/IVA
    # já vêm tratados em dados_fatura e são aplicados em _montar_resposta,
    # exactamente como no caminho com IA.
    def _classificar_com_regras(self, text: str, dados_fatura: Dict[str, Any]) -> Dict[str, Any]:
        t = (text or "").lower()

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
            "descricao": (text or "")[:150].replace("\n", " ").strip(),
            "confianca": 65 if tipo != pgc_ao.TIPO_A_CLASSIFICAR else 40,
            "modelo": "regras",
            "fundamentacao": "",
        }
