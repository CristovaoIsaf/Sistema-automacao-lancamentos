"""
[LEGADO — caminho RAG à mão, substituído pelo AnythingLLM em document_analyzer.py.
Preservado para comparação na tese; não é usado pelo fluxo actual.]

Constrói o índice de embeddings do corpus do PGC-AO (rag/pgc_corpus.jsonl).

Corre isto sempre que o corpus mudar, a partir de fastapi/app:

    python -m legacy.ingest

Requer o Ollama a correr localmente com o modelo nomic-embed-text já obtido
(ollama pull nomic-embed-text).
"""

import logging

from legacy.retriever import construir_indice

logging.basicConfig(level=logging.INFO, format="%(message)s")

if __name__ == "__main__":
    construir_indice()
