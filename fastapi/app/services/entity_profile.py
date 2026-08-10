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
from typing import Any, Dict, Optional, Set

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


def resumo(nif: Optional[str]) -> Dict[str, Any]:
    """Fase 12 do plano de 20 fases ("Perfil de Entidade" — "reutilizar
    conhecimento"): até esta fase, o conhecimento acumulado por
    `registrar_classificacao` só era usado internamente para poupar
    chamadas de IA (ver document_analyzer.py) — nunca ficava visível a
    quem consulta o dossiê de uma entidade (ver EntidadeController no
    lado Java). `resumo` expõe o mesmo estado já mantido por este módulo
    (a "distribuição" é a mesma Counter que decide `tipo_dominante`, não
    um cálculo novo/duplicado), para o Java o poder anexar ao dossiê.

    `distribuicaoTiposDocumento` — quais tipos de documento esta
    entidade costuma enviar (não só o tipo dominante, quando o histórico
    ainda não é unânime), e `totalDocumentos` — quantos documentos já
    contribuíram (excluindo reanálises do mesmo fingerprint, ver
    `registrar_classificacao`). Devolve zeros/None para uma entidade sem
    histórico — nunca inventa dados."""
    if not nif:
        return {"nif": nif, "totalDocumentos": 0, "tipoDominante": None, "distribuicaoTiposDocumento": {}}

    with _lock:
        contagem = _perfis.get(nif)
        if not contagem:
            return {"nif": nif, "totalDocumentos": 0, "tipoDominante": None, "distribuicaoTiposDocumento": {}}
        distribuicao = dict(contagem)

    total = sum(distribuicao.values())
    tipo_dominante_atual = tipo_dominante(nif)
    return {
        "nif": nif,
        "totalDocumentos": total,
        "tipoDominante": tipo_dominante_atual,
        "distribuicaoTiposDocumento": distribuicao,
    }
