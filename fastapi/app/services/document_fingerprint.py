"""
Fingerprint documental (Fase 4 do mapa de impacto) — fonte única para
calcular a identidade de conteúdo de um ficheiro.

Mesmo algoritmo (SHA-256, hex minúsculo) já usado do lado Java em
DocumentoController.calcularHash, que grava o resultado em
DocumentoContabilistico.hashConteudo (UNIQUE) no momento do upload. Este
módulo NÃO substitui esse cálculo — no fluxo real, o backend Java já
conhece o hash (calculou-o uma vez no upload) e envia-o em cada chamada a
POST /analisar (ver FastApiAnaliseClient), para não recalcular à toa nem
arriscar duas implementações a divergirem. `calcular_fingerprint` existe
para os casos em que o chamador NÃO o forneceu (chamada directa/manual ao
FastAPI, fora do fluxo Java) e como base para o cache de OCR/validação
das próximas fases (5/6), que precisam de uma chave de identidade estável
por conteúdo.
"""

import hashlib

FINGERPRINT_ALGORITHM = "sha256"


def calcular_fingerprint(conteudo: bytes) -> str:
    """SHA-256 dos bytes, em hexadecimal minúsculo — mesmo formato que
    DocumentoController.calcularHash produz do lado Java (String.format
    "%02x" por byte, também minúsculo)."""
    return hashlib.sha256(conteudo).hexdigest()
