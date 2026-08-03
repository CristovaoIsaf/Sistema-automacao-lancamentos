"""
[LEGADO — caminho RAG à mão, substituído pelo AnythingLLM em document_analyzer.py.
Preservado para comparação na tese; não é usado pelo fluxo actual.]

Cliente do LLM local via Ollama, chamado directamente (sem AnythingLLM pelo
meio). Modelo pequeno (llama3.2:1b) escolhido por restrições reais de
disco/RAM/CPU desta máquina de desenvolvimento (sem GPU dedicada).
"""

import logging

import httpx

logger = logging.getLogger(__name__)

OLLAMA_BASE_URL = "http://localhost:11434"
CHAT_MODEL = "llama3.2:1b"


def gerar_resposta(prompt: str) -> str:
    """Chama o Ollama local (CHAT_MODEL) e devolve o texto gerado."""
    resposta = httpx.post(
        f"{OLLAMA_BASE_URL}/api/generate",
        json={"model": CHAT_MODEL, "prompt": prompt, "stream": False},
        timeout=120.0,
    )
    resposta.raise_for_status()
    return resposta.json()["response"]


def ollama_disponivel() -> bool:
    try:
        resposta = httpx.get(f"{OLLAMA_BASE_URL}/api/tags", timeout=3.0)
        return resposta.status_code == 200
    except httpx.HTTPError:
        return False
