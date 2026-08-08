"""
Perfil de entidade / fornecedor (Fase 10 do mapa de impacto) — aprende a
classificação de operação típica de uma entidade (identificada pelo NIF
do emitente) a partir de documentos já classificados com confiança, para
poupar chamadas de IA em documentos SEGUINTES da MESMA entidade.

ATENÇÃO (secção 9 do mapa de impacto — "documentos semelhantes"): não
assumir que documentos parecidos são o mesmo documento, nem reutilizar
valores concretos (valor, data, nº) de outro documento — só a TENDÊNCIA
de classificação é reaproveitada, nunca dados. Por isso:
  - exige um número mínimo de documentos já vistos (MINIMO_DOCUMENTOS);
  - exige UNANIMIDADE: uma entidade com histórico misto (ex: é
    fornecedor numa compra, mas também aparece como cliente noutra
    transação) nunca produz um "tipo dominante" — cai sempre de volta ao
    caminho normal (regras/IA), nunca força uma classificação duvidosa.

Em memória de processo — mesmo racional das Fases 5/6/9 (ver
cache_versionado.py): suficiente para o âmbito actual, sem dependência
nova (Redis/BD).
"""

from collections import Counter, defaultdict
from threading import Lock
from typing import Dict, Optional, Set

from services import pgc as pgc_ao

MINIMO_DOCUMENTOS = 3

_perfis: Dict[str, Counter] = defaultdict(Counter)
_fingerprints_registados: Set[str] = set()
_lock = Lock()


def registrar_classificacao(
    nif: Optional[str], tipo: Optional[str], fingerprint: Optional[str] = None
) -> None:
    """Regista uma classificação já decidida (nunca TIPO_A_CLASSIFICAR)
    no perfil da entidade `nif`.

    Idempotente por `fingerprint`: reanalisar o MESMO documento (mesmo
    fingerprint) não conta uma segunda vez — sem isto, um único
    documento reanalisado várias vezes dominaria artificialmente o
    perfil da entidade. Sem fingerprint, regista sempre (chamador fora
    do fluxo normal)."""
    if not nif or not tipo or tipo == pgc_ao.TIPO_A_CLASSIFICAR:
        return

    with _lock:
        if fingerprint:
            if fingerprint in _fingerprints_registados:
                return
            _fingerprints_registados.add(fingerprint)
        _perfis[nif][tipo] += 1


def tipo_dominante(nif: Optional[str]) -> Optional[str]:
    """Tipo de operação já estabelecido para esta entidade, ou None se
    não houver perfil de confiança suficiente (poucos documentos, ou
    histórico misto — ver nota do módulo)."""
    if not nif:
        return None

    with _lock:
        contagem = _perfis.get(nif)
        if not contagem:
            return None
        if sum(contagem.values()) < MINIMO_DOCUMENTOS:
            return None
        if len(contagem) > 1:
            return None  # histórico misto — não decide sozinho

        (tipo_unico,) = contagem.keys()
        return tipo_unico
