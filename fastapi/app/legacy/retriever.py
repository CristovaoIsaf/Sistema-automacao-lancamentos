"""
[LEGADO — caminho RAG à mão, substituído pelo AnythingLLM em document_analyzer.py.
Preservado para comparação na tese; não é usado pelo fluxo actual.]

Retrieval por similaridade de cosseno sobre o corpus real do Decreto n.º 82/01
(rag/pgc_corpus.jsonl). Os embeddings são gerados pelo Ollama local (nomic-embed-text).

O índice (vectores + metadados) é pré-calculado por `legacy/ingest.py` e gravado
em disco — não se recalcula a cada pedido, só quando o corpus muda.
"""

import json
import logging
from pathlib import Path
from typing import Dict, List

import httpx
import numpy as np

logger = logging.getLogger(__name__)

# Ficheiro movido para legacy/ (ver document_analyzer.py, que agora usa o
# AnythingLLM), mas os dados reais do corpus continuam em rag/ — não foram
# movidos, só o código do RAG à mão.
RAG_DIR = Path(__file__).resolve().parent.parent / "rag"
CORPUS_PATH = RAG_DIR / "pgc_corpus.jsonl"
INDEX_PATH = RAG_DIR / "pgc_index.npy"
INDEX_META_PATH = RAG_DIR / "pgc_index_meta.json"

OLLAMA_BASE_URL = "http://localhost:11434"
EMBED_MODEL = "nomic-embed-text"


def carregar_corpus() -> List[Dict]:
    chunks = []
    with open(CORPUS_PATH, "r", encoding="utf-8") as f:
        for linha in f:
            linha = linha.strip()
            if linha:
                chunks.append(json.loads(linha))
    return chunks


def obter_embedding(texto: str) -> np.ndarray:
    resposta = httpx.post(
        f"{OLLAMA_BASE_URL}/api/embeddings",
        json={"model": EMBED_MODEL, "prompt": texto},
        timeout=30.0,
    )
    resposta.raise_for_status()
    vetor = resposta.json()["embedding"]
    return np.array(vetor, dtype=np.float32)


def construir_indice() -> None:
    """Gera o índice de embeddings do corpus real e grava-o em disco (ver ingest.py)."""
    chunks = carregar_corpus()
    vetores = []
    for chunk in chunks:
        vetores.append(obter_embedding(chunk["texto"]))
        logger.info("Embutido: %s", chunk["id"])

    matriz = np.vstack(vetores)
    np.save(INDEX_PATH, matriz)
    with open(INDEX_META_PATH, "w", encoding="utf-8") as f:
        json.dump(chunks, f, ensure_ascii=False, indent=2)

    logger.info("Índice construído: %d excertos.", len(chunks))


class Retriever:
    """Carrega o índice já construído e faz retrieval por similaridade de cosseno."""

    def __init__(self):
        self.disponivel = INDEX_PATH.exists() and INDEX_META_PATH.exists()
        if not self.disponivel:
            logger.warning(
                "Índice RAG não encontrado — corre 'python -m rag.ingest' "
                "em fastapi/app antes de usar o RAG."
            )
            self.matriz = None
            self.chunks: List[Dict] = []
            return

        self.matriz = np.load(INDEX_PATH)
        with open(INDEX_META_PATH, "r", encoding="utf-8") as f:
            self.chunks = json.load(f)

    def pesquisar(self, query: str, k: int = 4) -> List[Dict]:
        """Devolve os k excertos do corpus mais relevantes para a query."""
        if not self.disponivel:
            return []

        vetor_query = obter_embedding(query)
        normas = np.linalg.norm(self.matriz, axis=1) * np.linalg.norm(vetor_query)
        normas[normas == 0] = 1e-8
        similaridades = (self.matriz @ vetor_query) / normas

        indices_top = np.argsort(-similaridades)[:k]
        return [self.chunks[i] for i in indices_top]
