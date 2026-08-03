"""
Cliente REST do AnythingLLM — substitui o RAG feito à mão (ver
fastapi/app/legacy/retriever.py e legacy/llm_local.py) como motor de
classificação. O AnythingLLM já gere o Ollama, os embeddings e o vector
store (LanceDB) por trás da sua própria API.

Dois workspaces separados são consultados (ver document_analyzer.py):
NORMAS (fundamentação legal) e EXEMPLOS (orientação da classificação por
semelhança). Cada pedido é independente, em modo "query" (sem memória de
conversa) — por isso todas as funções aqui recebem o workspace explicitamente,
em vez de assumir um só configurado globalmente.
"""

import logging
from typing import Any, Dict, List

import httpx

from config.settings import settings

logger = logging.getLogger(__name__)


class AnythingLLMError(Exception):
    """Erro ao comunicar com o AnythingLLM (rede, autenticação, workspace, etc.)."""


def disponivel() -> bool:
    try:
        resposta = httpx.get(f"{settings.ANYTHINGLLM_BASE_URL}/api/ping", timeout=3.0)
        return resposta.status_code == 200
    except httpx.HTTPError:
        return False


def consultar_workspace(mensagem: str, workspace: str) -> Dict[str, Any]:
    """
    Envia uma pergunta a um workspace específico, em modo "query" — cada
    pedido é independente, sem memória de conversa.

    Devolve {"resposta": str, "fontes": List[str]}.
    Lança AnythingLLMError com mensagem clara em caso de falha.
    """
    if not settings.ANYTHINGLLM_API_KEY:
        raise AnythingLLMError("ANYTHINGLLM_API_KEY não está configurada — define-a no .env.")

    url = f"{settings.ANYTHINGLLM_BASE_URL}/api/v1/workspace/{workspace}/chat"

    try:
        resposta = httpx.post(
            url,
            headers={"Authorization": f"Bearer {settings.ANYTHINGLLM_API_KEY}"},
            json={"message": mensagem, "mode": "query"},
            timeout=300.0,
        )
    except httpx.ConnectError as e:
        raise AnythingLLMError(
            f"AnythingLLM não está a responder em {settings.ANYTHINGLLM_BASE_URL} "
            f"— confirma que a aplicação está a correr."
        ) from e
    except httpx.TimeoutException as e:
        raise AnythingLLMError(
            f"AnythingLLM demorou demasiado tempo a responder (workspace '{workspace}')."
        ) from e

    if resposta.status_code == 401:
        raise AnythingLLMError("API key do AnythingLLM inválida ou expirada.")
    if resposta.status_code == 404:
        raise AnythingLLMError(f"Workspace '{workspace}' não encontrado no AnythingLLM.")
    resposta.raise_for_status()

    dados = resposta.json()

    if dados.get("error"):
        raise AnythingLLMError(f"AnythingLLM devolveu um erro: {dados['error']}")

    texto = dados.get("textResponse", "") or ""
    fontes = _extrair_fontes(dados.get("sources") or [])

    if not fontes:
        logger.warning(
            "AnythingLLM devolveu 0 fontes — o workspace '%s' pode não ter "
            "documentos embutidos (upload != embutir no workspace, ver UI).",
            workspace,
        )

    return {"resposta": texto, "fontes": fontes}


def _extrair_fontes(sources: List[Dict[str, Any]]) -> List[str]:
    """
    As 'sources' do AnythingLLM trazem metadados do documento/chunk usado.
    Os nomes exactos dos campos podem variar por versão — por isso este
    parser tenta várias chaves plausíveis em vez de assumir uma única.
    Verificar contra uma resposta real (com fontes não vazias) assim que o
    workspace tiver conteúdo embutido.
    """
    citacoes: List[str] = []
    for fonte in sources:
        titulo = fonte.get("title") or fonte.get("docId") or fonte.get("docSourceType") or "documento"
        trecho = (fonte.get("text") or fonte.get("chunk") or "")[:150].strip().replace("\n", " ")
        citacoes.append(f"{titulo}: “{trecho}…”" if trecho else str(titulo))
    return citacoes
