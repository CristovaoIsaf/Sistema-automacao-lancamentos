import json
import logging
import re
from typing import Any, Dict, List, Optional, Tuple

from config.settings import settings
from services import anythingllm_client
from services import entity_profile
from services import pgc as pgc_ao
from services.cache_versionado import CacheVersionado

logger = logging.getLogger(__name__)

# Fase 9 do mapa de impacto — evita repetir a chamada de IA (a mais lenta
# e cara do pipeline) para reanálise do MESMO documento ambíguo (mesmo
# fingerprint). Reaproveita o mecanismo genérico já usado pelas Fases 5/6
# (cache_versionado.py) — não duplicar o mecanismo de cache uma terceira
# vez. IMPORTANTE: sempre que _construir_prompt_classificacao mudar de
# forma que altere o significado da pergunta (não só cosmética), subir
# AI_CACHE_VERSION — senão uma resposta antiga, calculada com um prompt
# diferente, seria confundida com uma actual.
# v2 — Fase 5 do plano de 20 fases (classificação contabilística) passou a
# incluir o contexto da empresa (atividade/natureza do negócio) no prompt.
# Fase 11 — a versão efetiva do cache (ver _classificar_com_anythingllm_cacheado)
# passou também a incluir _versao_contexto_empresa(contexto_empresa): o
# CONTEÚDO do contexto (não só a versão do prompt) também invalida o cache
# quando muda, sem precisar de subir esta constante a cada edição em
# /configuracoes.
AI_CACHE_VERSION = "TFC-2026-v2"
_cache_ia: CacheVersionado = CacheVersionado("ia")

# Fase 5 do plano de 20 fases — palavras que, na atividade económica/
# natureza do negócio da empresa (texto livre, configurado por um
# Administrador em /configuracoes), sugerem que a empresa presta serviços
# em vez de revender mercadorias. Usado só como desempate determinístico
# (nunca por IA) quando uma compra é genérica (não diz "mercadoria" nem
# tem palavras de serviço no PRÓPRIO documento) — ver
# _classificar_com_regras. Não classificar só pelo nome do produto: a
# mesma palavra "fatura" significa coisas diferentes consoante o negócio.
PALAVRAS_CHAVE_EMPRESA_SERVICOS = ["serviço", "servico", "prestação", "prestacao", "consultoria", "assessoria"]


def _versao_contexto_empresa(contexto_empresa: Optional[Dict[str, Any]]) -> str:
    """Fase 11 do plano de 20 fases ("Cache e IA"): o contexto da empresa
    (atividade/natureza — Fase 5) entra no prompt enviado à IA
    (_construir_prompt_classificacao), logo influencia a classificação
    devolvida. Sem isto na versão do cache, um Administrador que altere a
    atividade/natureza em /configuracoes faria reanálises do MESMO
    documento (mesmo fingerprint) continuarem a devolver uma classificação
    calculada com o contexto ANTIGO — o cache de IA nunca invalidaria,
    porque a chave (fingerprint, versão) não via a mudança. Mesmo
    princípio já aplicado a AI_CACHE_VERSION/workspaces (ver
    cache_versionado.py: "uma entrada antiga nunca é devolvida como se
    fosse actual")."""
    if not contexto_empresa:
        return "sem_contexto"
    atividade = contexto_empresa.get("atividade_economica") or ""
    natureza = contexto_empresa.get("natureza_negocio") or ""
    return f"{atividade}|{natureza}"


def _empresa_e_prestadora_de_servicos(contexto_empresa: Optional[Dict[str, Any]]) -> bool:
    if not contexto_empresa:
        return False
    texto = " ".join(filter(None, [
        contexto_empresa.get("atividade_economica"),
        contexto_empresa.get("natureza_negocio"),
    ])).lower()
    return any(p in texto for p in PALAVRAS_CHAVE_EMPRESA_SERVICOS)

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
    def analyze_document(
        self,
        text: str,
        ocr_data: Dict[str, Any],
        fingerprint: Optional[str] = None,
        contexto_empresa: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """`contexto_empresa` (Fase 5 do plano de 20 fases — classificação
        contabilística): {"atividade_economica": ..., "natureza_negocio":
        ...}, enviado pelo backend Java em TODA análise (sempre disponível,
        ao contrário do histórico da entidade — ver ContextoClassificacaoService
        no lado Java). Usado como desempate nas regras e como contexto
        adicional no prompt de IA — nunca decide sozinho, nunca inventado."""
        dados_fatura = ocr_data.get("dados_fatura", {}) or {}

        # Fase 7 do mapa de impacto — gate antes da IA: a classificação por
        # regras é determinística, sem I/O e praticamente grátis — corre-se
        # sempre primeiro. Só se as regras não conseguirem decidir (caem em
        # TIPO_A_CLASSIFICAR, nenhuma palavra-chave reconhecida) é que vale a
        # pena gastar uma chamada de IA a tentar desambiguar. Quando as
        # regras já reconheceram um padrão claro, esse resultado é usado tal
        # como está — a IA não traria mais informação nesse caso.
        classificacao_regras = self._classificar_com_regras(text, dados_fatura, contexto_empresa)

        if not self._precisa_de_ia(classificacao_regras):
            logger.info(
                "Classificação por regras já é inequívoca (%s) — IA não chamada.",
                classificacao_regras.get("tipo"),
            )
            entity_profile.registrar_classificacao(
                dados_fatura.get("emitente_nif"), classificacao_regras.get("tipo"), fingerprint
            )
            resposta = self._montar_resposta(classificacao_regras, dados_fatura)
            resposta["iaCacheHit"] = False
            return resposta

        # Fase 10 — antes de gastar uma chamada de IA, ver se a entidade
        # emitente já tem um histórico consistente de classificação (ver
        # entity_profile.py). Corre depois do gate da Fase 7 (só entra
        # aqui quando as regras já não conseguiram decidir sozinhas).
        classificacao_perfil = self._classificar_por_perfil_entidade(text, dados_fatura)
        if classificacao_perfil is not None:
            entity_profile.registrar_classificacao(
                dados_fatura.get("emitente_nif"), classificacao_perfil.get("tipo"), fingerprint
            )
            resposta = self._montar_resposta(classificacao_perfil, dados_fatura)
            resposta["iaCacheHit"] = False
            return resposta

        ia_cache_hit = False
        try:
            if self.use_ai:
                classificacao, ia_cache_hit = self._classificar_com_anythingllm_cacheado(
                    text, dados_fatura, fingerprint, contexto_empresa
                )
            else:
                classificacao = classificacao_regras
        except Exception as e:
            logger.error(f"Erro na classificação, a usar regras: {e}")
            classificacao = classificacao_regras

        entity_profile.registrar_classificacao(
            dados_fatura.get("emitente_nif"), classificacao.get("tipo"), fingerprint
        )

        resposta = self._montar_resposta(classificacao, dados_fatura)
        resposta["iaCacheHit"] = ia_cache_hit
        return resposta

    def _precisa_de_ia(self, classificacao_regras: Dict[str, Any]) -> bool:
        """Fase 7 — só é preciso chamar a IA quando a classificação por
        regras não conseguiu decidir o tipo de operação (caiu no valor
        genérico TIPO_A_CLASSIFICAR)."""
        return classificacao_regras.get("tipo") == pgc_ao.TIPO_A_CLASSIFICAR

    def _classificar_por_perfil_entidade(
        self, text: str, dados_fatura: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        """Fase 10 — se a entidade emitente já tem um histórico
        consistente de classificação (ver entity_profile.py), reaproveita
        essa tendência em vez de chamar a IA. None se não houver perfil
        de confiança suficiente (poucos documentos, ou histórico misto)."""
        nif = dados_fatura.get("emitente_nif")
        tipo = entity_profile.tipo_dominante(nif)
        if not tipo:
            return None

        logger.info(
            "Entidade NIF=%s já tem classificação consistente (%s) — IA não chamada.", nif, tipo
        )
        return {
            "tipo": tipo,
            "descricao": (text or "")[:150].replace("\n", " ").strip(),
            "confianca": 75,
            "modelo": "perfil_entidade",
            "fundamentacao": "",
        }

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
            # Fase 10 do plano de 20 fases ("pesquisa por número; série"):
            # já vinha extraído por regex_extract.py (numero_fatura, ex.
            # "FT 2026/001" — série+número combinados) mas nunca tinha sido
            # incluído aqui, ficava sempre perdido antes de chegar ao Java.
            "numeroDocumento": dados_fatura.get("numero_fatura"),
            "confianca": c.get("confianca", 70),
            "modelo": c.get("modelo", "regras"),
            "fundamentacao": c.get("fundamentacao", ""),
            "categoria": pgc_ao.categoria_do_tipo(tipo),
            "linhas": linhas,
            # Fase 4 do plano de 20 fases — contextualização assistida: só
            # preenchido quando o sistema realmente não conseguiu decidir
            # sozinho (nunca quando já há confiança suficiente).
            "perguntaContextualizacao": (
                self._construir_pergunta_contextualizacao(dados_fatura)
                if tipo == pgc_ao.TIPO_A_CLASSIFICAR else None
            ),
        }

    def _construir_pergunta_contextualizacao(self, dados_fatura: Dict[str, Any]) -> Dict[str, Any]:
        """Fase 4 — contextualização assistida: quando regras + perfil + IA
        não conseguiram decidir o tipo de operação, pergunta UMA coisa
        específica ao contabilista em vez de o deixar a rever tudo às
        cegas. Não é um chatbot genérico — pergunta fechada, com opções
        fixas e um resultado (linhas) já pré-calculado por opção, para o
        frontend aplicar a escolha instantaneamente sem novo pedido ao
        FastAPI.

        Só oferece opções que o pgc_ao consiga mesmo construir com uma
        conta real do Decreto 82/01 (mercadoria, serviço). "Ativo para
        utilização"/"equipamento administrativo" — do exemplo original do
        mapa de impacto — não têm conta de imobilizado modelada neste
        TFC; nunca inventar aqui um código de conta não verificado. É
        mais correcto o contabilista escolher "Outro" e rever manualmente
        do que o sistema sugerir uma conta errada com ar de confiança.
        """
        valor = dados_fatura.get("valor_total_aoa") or "0"
        taxa_iva = self._taxa_iva_tratada(dados_fatura)

        candidatos = [
            ("mercadoria_revenda", "Mercadoria para revenda", pgc_ao.TIPO_COMPRA_MERCADORIA),
            ("servico", "Serviço", pgc_ao.TIPO_COMPRA_SERVICO),
        ]

        opcoes = []
        for valor_opcao, rotulo, tipo_opcao in candidatos:
            opcoes.append({
                "valor": valor_opcao,
                "rotulo": rotulo,
                "tipo": tipo_opcao,
                "categoria": pgc_ao.categoria_do_tipo(tipo_opcao),
                "linhas": pgc_ao.construir_lancamento(tipo_opcao, valor, "", taxa_iva=taxa_iva),
            })

        opcoes.append({
            "valor": "outro",
            "rotulo": "Outro / não tenho a certeza — rever manualmente",
            "tipo": pgc_ao.TIPO_A_CLASSIFICAR,
            "categoria": pgc_ao.categoria_do_tipo(pgc_ao.TIPO_A_CLASSIFICAR),
            "linhas": pgc_ao.construir_lancamento(pgc_ao.TIPO_A_CLASSIFICAR, valor, ""),
        })

        return {
            "pergunta": "Qual a finalidade deste documento?",
            "opcoes": opcoes,
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
        """Só reconhece as taxas de IVA em pgc_ao.TAXAS_IVA_RECONHECIDAS
        (fonte única — 14%/7%, as taxas em vigor em Angola tratadas por
        este projeto) entre as taxas já detetadas por regex em
        regex_extract.taxas_iva_encontradas — sem re-extrair."""
        for taxa in dados_fatura.get("taxas_iva_encontradas") or []:
            taxa_normalizada = str(taxa).replace(",", ".").split(".")[0]
            if taxa_normalizada in pgc_ao.TAXAS_IVA_RECONHECIDAS:
                return taxa_normalizada
        return ""

    # ── Classificação via AnythingLLM: 2 workspaces separados ────────────────
    #   1) EXEMPLOS decide a classificação (tipo), por semelhança com
    #      documentos de exemplo já classificados — usando dados já tratados,
    #      não o texto em bruto como fonte primária.
    #   2) NORMAS só é consultado depois, para fundamentar legalmente o tipo
    #      já decidido — falha aqui não deita fora uma classificação válida.
    def _classificar_com_anythingllm(
        self, text: str, dados_fatura: Dict[str, Any], contexto_empresa: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        prompt = self._construir_prompt_classificacao(text, dados_fatura, contexto_empresa)
        logger.info(
            "Prompt de classificação: %d caracteres (~%d tokens estimados, ~4 chars/token).",
            len(prompt), len(prompt) // 4,
        )

        resultado = anythingllm_client.consultar_workspace(prompt, settings.ANYTHINGLLM_WORKSPACE_EXEMPLOS)
        dados = self._extrair_json(resultado["resposta"])

        dados["modelo"] = f"anythingllm:{settings.ANYTHINGLLM_WORKSPACE_EXEMPLOS}+{settings.ANYTHINGLLM_WORKSPACE_NORMAS}"
        dados["fundamentacao"] = self._obter_fundamentacao(dados.get("tipo"), resultado["fontes"])

        logger.info("AnythingLLM (exemplos) classificou como: %s", dados.get("tipo"))
        return dados

    def _classificar_com_anythingllm_cacheado(
        self,
        text: str,
        dados_fatura: Dict[str, Any],
        fingerprint: Optional[str],
        contexto_empresa: Optional[Dict[str, Any]] = None,
    ) -> Tuple[Dict[str, Any], bool]:
        """Fase 9: variante de _classificar_com_anythingllm que evita
        repetir as DUAS chamadas de IA (classificação + fundamentação
        legal) para o mesmo documento ambíguo já analisado antes. Sem
        fingerprint, chama sempre a IA de novo — não há chave de cache
        válida. Devolve (resultado, veio_do_cache)."""
        if not fingerprint:
            return self._classificar_com_anythingllm(text, dados_fatura, contexto_empresa), False

        versao = (
            f"{AI_CACHE_VERSION}:{settings.ANYTHINGLLM_WORKSPACE_EXEMPLOS}:{settings.ANYTHINGLLM_WORKSPACE_NORMAS}"
            f":{_versao_contexto_empresa(contexto_empresa)}"
        )
        resultado, veio_do_cache = _cache_ia.obter_ou_calcular(
            fingerprint, versao, lambda: self._classificar_com_anythingllm(text, dados_fatura, contexto_empresa)
        )
        if veio_do_cache:
            logger.info("Classificação de IA reaproveitada do cache (fingerprint=%s).", fingerprint)
        return resultado, veio_do_cache

    # Fase 8 do mapa de impacto — otimização do prompt: só chega aqui quando
    # o gate da Fase 7 já decidiu que a IA é mesmo necessária (regras não
    # conseguiram classificar), por isso o excerto de texto continua
    # generoso (2000 caracteres — cortá-lo mais arriscaria tirar
    # precisamente o contexto que falta nos casos difíceis, sem medição
    # real que justifique um valor menor). O que É reduzido, sem qualquer
    # perda de informação, é o texto fixo (instruções + rótulos), enviado
    # em TODAS as chamadas de IA — extraído para um método próprio para
    # poder ser testado isoladamente (tamanho, presença dos campos).
    def _construir_prompt_classificacao(
        self, text: str, dados_fatura: Dict[str, Any], contexto_empresa: Optional[Dict[str, Any]] = None
    ) -> str:
        texto = (text or "")[:2000]

        # Fase 5 do plano de 20 fases — contexto da empresa (configurado
        # por um Administrador, nunca inventado), só incluído quando
        # existe. Ajuda a IA a não classificar só pelo "nome do produto"
        # (ex: "viatura" não é sempre mercadoria — depende do negócio).
        linha_empresa = ""
        if contexto_empresa and (contexto_empresa.get("atividade_economica") or contexto_empresa.get("natureza_negocio")):
            linha_empresa = (
                f"\nCONTEXTO DA EMPRESA (usar só para desambiguar, não é um dado do documento):\n"
                f"- Atividade económica: {contexto_empresa.get('atividade_economica') or '?'}\n"
                f"- Natureza do negócio: {contexto_empresa.get('natureza_negocio') or '?'}\n"
            )

        return f"""Especialista em contabilidade angolana (PGC-AO, Decreto 82/01). Os dados abaixo já foram extraídos por regex — não os reinterprete nem corrija. Tarefa: CLASSIFICAR o tipo de operação contabilística. Não indique contas nem valores.

DADOS EXTRAÍDOS:
- Emitente: {dados_fatura.get('emitente_nome') or '(não identificado)'} (NIF {dados_fatura.get('emitente_nif') or '?'})
- Adquirente: {dados_fatura.get('adquirente_nome') or '(não identificado)'} (NIF {dados_fatura.get('adquirente_nif') or '?'})
- Nº documento: {dados_fatura.get('numero_fatura') or '?'}
- Valor total: {dados_fatura.get('valor_total_aoa') or '?'} AOA
- Data: {dados_fatura.get('data_emissao') or '?'}
- Tipo de documento detetado: {dados_fatura.get('tipo_documento') or 'desconhecido'}
{linha_empresa}
EXCERTO (contexto/desambiguação, não extrair dados novos):
{texto}

Responda só com JSON válido, sem texto à volta:
{{"tipo": "compra_mercadoria|compra_servico|venda_mercadoria|prestacao_servico|pagamento_fornecedor|recebimento_cliente|a_classificar", "descricao": "curta", "confianca": 0}}"""

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
    def _classificar_com_regras(
        self, text: str, dados_fatura: Dict[str, Any], contexto_empresa: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        t = (text or "").lower()

        recebimento = any(k in t for k in ["recibo de pagamento", "recebido", "recebemos", "entrada de caixa"])
        pagamento = any(k in t for k in ["pagamento a", "pago a fornecedor", "comprovativo de pagamento", "saída de caixa"])
        venda = any(k in t for k in ["fatura de venda", "factura de venda", "venda", "vendemos", "cliente"])
        servico = any(k in t for k in ["serviço", "servico", "prestação", "prestacao", "renda", "aluguer",
                                       "água", "agua", "energia", "electricidade", "eletricidade", "transporte",
                                       "seguro", "telefone", "internet"])
        mercadoria_especifica = "mercadoria" in t
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
            # Fase 5 do plano de 20 fases — "não classificar apenas pelo
            # nome do produto": uma "fatura"/"compra" genérica, sem a
            # palavra "mercadoria" nem palavras de serviço no PRÓPRIO
            # documento, não é automaticamente mercadoria para revenda —
            # se a empresa é conhecida por prestar serviços (contexto
            # configurado, nunca inferido pela IA), é mais provável ser
            # uma despesa operacional.
            if not mercadoria_especifica and _empresa_e_prestadora_de_servicos(contexto_empresa):
                tipo = pgc_ao.TIPO_COMPRA_SERVICO
            else:
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
