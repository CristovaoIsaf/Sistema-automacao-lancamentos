"""
Matriz de campos obrigatórios por tipo de documento — fonte única de
verdade, versionada, para o relatório de qualidade da extracção
(ver regex_extract.py::extrair_dados_fatura/gerar_relatorio_qualidade).

Antes desta consolidação, existia UMA única lista fixa de "campos
obrigatórios" (pensada para factura) aplicada indiscriminadamente a
qualquer tipo de documento detectado (recibo, nota de crédito, nota de
débito, ...) — um recibo era sempre marcado como incompleto por não ter
NIF do adquirente ou código hash AGT, mesmo quando isso nunca foi um
requisito real para um recibo.

Os nomes de campo abaixo correspondem exactamente aos atributos de
DadosFatura (regex_extract.py) — esta matriz não inventa nenhum campo
novo, só decide QUAIS dos campos já extraídos são obrigatórios para CADA
tipo de documento.

IMPORTANTE (mesma nota já usada em pgc.py para a IVA): estas listas são
uma decisão do projecto para efeitos do TFC, não uma transcrição literal
de uma circular ou instrução normativa da AGT com número identificado —
não inventar aqui uma referência legal ("AGT-XX-XX-vN") sem confirmação.
"""

from typing import Dict, List, Optional

# Versão desta matriz — qualquer alteração às listas abaixo deve
# incrementar esta versão, para que resultados de qualidade antigos nunca
# sejam confundidos com resultados calculados por uma matriz diferente
# (ver Sugestao/Análise no lado Java, que pode vir a guardar esta versão
# junto do resultado).
DOCUMENT_REQUIREMENTS_VERSION = "TFC-2026-v1"

# Campos exigidos para uma factura "completa" — o conjunto mais rigoroso,
# porque é o único tipo com hash de certificação AGT já extraído.
_REQUISITOS_FATURA = [
    "emitente_nome",
    "emitente_nif",
    "adquirente_nif",
    "numero_fatura",
    "data_emissao",
    "valor_total_aoa",
    "codigo_hash",
]

REQUISITOS_POR_TIPO: Dict[str, List[str]] = {
    "fatura": _REQUISITOS_FATURA,
    "fatura_recibo": _REQUISITOS_FATURA,
    # Recibo: não exige NIF do adquirente nem código hash — nem sempre
    # presentes num recibo simples, ao contrário de uma factura emitida
    # por software certificado.
    "recibo": [
        "emitente_nome",
        "emitente_nif",
        "numero_fatura",
        "data_emissao",
        "valor_total_aoa",
    ],
    # Nota de crédito/débito: mesma exigência de identificação das partes
    # que uma factura, mas sem o hash (frequentemente emitidas por outros
    # meios que não o software de facturação certificado da factura original).
    "nota_credito": [
        "emitente_nome",
        "emitente_nif",
        "adquirente_nif",
        "numero_fatura",
        "data_emissao",
        "valor_total_aoa",
    ],
    "nota_debito": [
        "emitente_nome",
        "emitente_nif",
        "adquirente_nif",
        "numero_fatura",
        "data_emissao",
        "valor_total_aoa",
    ],
    # Guia de pagamento / extrato: documentos de tesouraria, não fiscais —
    # exigência mínima, só o essencial para identificar a operação.
    "guia_pagamento": ["emitente_nome", "data_emissao", "valor_total_aoa"],
    "extrato": ["emitente_nome", "data_emissao"],
}


def requisitos_para(tipo_documento: Optional[str]) -> List[str]:
    """Lista de campos obrigatórios (nomes de atributos de DadosFatura)
    para o tipo de documento indicado. Tipo desconhecido/não detectado
    cai no conjunto mais rigoroso (factura) — mais seguro assumir
    demasiados campos em falta do que validar de menos um documento que
    o sistema nem sequer conseguiu identificar."""
    return REQUISITOS_POR_TIPO.get(tipo_documento, _REQUISITOS_FATURA)
