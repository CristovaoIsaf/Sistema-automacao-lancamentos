"""
Plano Geral de Contabilidade Angolano (PGC-AO) — Decreto n.º 82/01, de 16 de Novembro.

FONTE ÚNICA E OFICIAL deste projeto. Todos os códigos de conta abaixo constam
literalmente do quadro e lista de contas do Decreto 82/01. Não acrescentar aqui
nenhuma conta que não exista nesse decreto.

IVA — NOTA IMPORTANTE
---------------------
O Decreto 82/01 é de 2001, anterior à entrada em vigor do IVA em Angola (2019).
Por isso o decreto NÃO define nenhuma conta oficial de IVA. A conta 34 «Estado»
desdobra-se, no decreto, apenas em:
    34.1 Imposto sobre os lucros
    34.2 Imposto de produção e consumo
    34.3 Imposto de rendimento de trabalho
    34.4 Imposto de circulação
    34.8 Subsídios a preços
    34.9 Outros impostos

34.5.1 (IVA dedutível) e 34.5.2 (IVA liquidado) são uma EXTENSÃO deste projeto,
indicada explicitamente pelo autor do TFC — não constam literalmente do texto
do Decreto 82/01. Seguem a numeração já usada pelo decreto para outros
impostos sob a conta 34, mas devem ser identificadas como decisão do projeto
(e não como citação do decreto) em qualquer texto da tese. Taxa aplicada: 14%
(taxa geral de IVA em Angola), assumindo que o valor extraído do documento já
inclui IVA (prática comum em faturas/recibos ao consumidor).
"""

from decimal import Decimal, ROUND_HALF_UP
from typing import Dict, List, Optional


# ── Contas oficiais usadas neste TFC (todas constam do Decreto 82/01) ─────────
# Classe 2 — Existências
C_COMPRAS = ("21", "Compras")
C_MERCADORIAS = ("26", "Mercadorias")
# Classe 3 — Terceiros
C_CLIENTES = ("31", "Clientes")
C_FORNECEDORES = ("32", "Fornecedores")
# Classe 4 — Meios monetários
C_BANCO = ("43", "Depósitos à ordem")
C_CAIXA = ("45", "Caixa")
# Classe 6 — Proveitos e ganhos por natureza
C_VENDAS = ("61", "Vendas")
C_PRESTACAO_SERVICOS = ("62", "Prestações de serviços")
# Classe 7 — Custos e perdas por natureza (Fornecimentos e serviços de terceiros: 75.2.xx)
C_FSE = ("75", "Outros custos e perdas operacionais")
C_FSE_AGUA = ("75.2.11", "Água")
C_FSE_ELECTRICIDADE = ("75.2.12", "Electricidade")
C_FSE_CONSERVACAO = ("75.2.14", "Conservação e reparação")
C_FSE_RENDAS = ("75.2.21", "Rendas e alugueres")
C_FSE_SEGUROS = ("75.2.22", "Seguros")
C_FSE_DESLOCACOES = ("75.2.23", "Deslocações e estadas")
C_FSE_COMUNICACAO = ("75.2.20", "Comunicação")

# Conta genérica quando o documento não pôde ser classificado com segurança.
# NÃO é um código do plano: sinaliza que precisa de revisão humana.
CONTA_A_CLASSIFICAR = ("A_CLASSIFICAR", "A classificar (revisão manual)")


# ── IVA (ativo — ver nota no topo: 34.5.1/34.5.2 são extensão do projeto,
#   indicada explicitamente pelo autor do TFC, não constam do Decreto 82/01) ──
IVA_ATIVO: bool = True
CONTA_IVA_DEDUTIVEL: Optional[tuple] = ("34.5.1", "IVA dedutível")
CONTA_IVA_LIQUIDADO: Optional[tuple] = ("34.5.2", "IVA liquidado")
TAXA_IVA: Optional[Decimal] = Decimal("0.14")


# ── Tipos de documento suportados (âmbito do TFC: compra, venda, pagamentos) ──
TIPO_COMPRA_MERCADORIA = "compra_mercadoria"
TIPO_COMPRA_SERVICO = "compra_servico"        # renda, água, energia, etc.
TIPO_VENDA_MERCADORIA = "venda_mercadoria"
TIPO_PRESTACAO_SERVICO = "prestacao_servico"
TIPO_PAGAMENTO_FORNECEDOR = "pagamento_fornecedor"
TIPO_RECEBIMENTO_CLIENTE = "recebimento_cliente"
TIPO_A_CLASSIFICAR = "a_classificar"

TIPOS_VALIDOS = {
    TIPO_COMPRA_MERCADORIA,
    TIPO_COMPRA_SERVICO,
    TIPO_VENDA_MERCADORIA,
    TIPO_PRESTACAO_SERVICO,
    TIPO_PAGAMENTO_FORNECEDOR,
    TIPO_RECEBIMENTO_CLIENTE,
    TIPO_A_CLASSIFICAR,
}


def _normalizar_numero(valor: str) -> str:
    """
    Normaliza um número em texto para o formato que o Decimal entende
    (separador decimal = ponto, sem separador de milhares).

    Há duas origens possíveis para `valor` neste módulo:
      - O Gemini, que já devolve "150000.00" (ponto decimal, sem milhares,
        conforme pedido no prompt) — passa tal como está.
      - O extrator por regex, que devolve o número tal como apareceu no
        documento, no formato angolano/PT-PT ("50.000,00": ponto = milhares,
        vírgula = decimal) — sem isto, Decimal("50.000,00") rebenta e o
        valor extraído era sempre substituído por 0.00.
    """
    valor = valor.strip()
    if "," in valor:
        valor = valor.replace(".", "").replace(",", ".")
    return valor


def _dec(valor) -> Decimal:
    """Converte para Decimal com 2 casas, tolerante a None/strings/formato PT-AO."""
    if valor is None or valor == "":
        return Decimal("0.00")
    try:
        return Decimal(_normalizar_numero(str(valor))).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except Exception:
        return Decimal("0.00")


def _linha(conta: tuple, debito: Decimal = None, credito: Decimal = None,
           descricao: str = "") -> Dict[str, str]:
    return {
        "conta": conta[0],
        "nome": conta[1],
        "debito": f"{debito:.2f}" if debito is not None else None,
        "credito": f"{credito:.2f}" if credito is not None else None,
        "descricao": descricao,
    }


def conta_meio_pagamento(texto: str) -> tuple:
    """Escolhe 45 Caixa ou 43 Depósitos à ordem consoante pistas no texto."""
    t = (texto or "").lower()
    if any(k in t for k in ["banco", "transferência", "transferencia", "iban", "depósito", "deposito"]):
        return C_BANCO
    return C_CAIXA


def conta_despesa_servico(texto: str) -> tuple:
    """Mapeia uma despesa/serviço para a sub-conta 75.2.xx mais adequada."""
    t = (texto or "").lower()
    if "água" in t or "agua" in t:
        return C_FSE_AGUA
    if any(k in t for k in ["energia", "electricidade", "eletricidade", "luz", "ende"]):
        return C_FSE_ELECTRICIDADE
    if any(k in t for k in ["renda", "aluguer", "arrendamento"]):
        return C_FSE_RENDAS
    if "seguro" in t:
        return C_FSE_SEGUROS
    if any(k in t for k in ["transporte", "viagem", "deslocação", "deslocacao", "combustível", "combustivel"]):
        return C_FSE_DESLOCACOES
    if any(k in t for k in ["telefone", "internet", "comunicação", "comunicacao"]):
        return C_FSE_COMUNICACAO
    if any(k in t for k in ["reparação", "reparacao", "manutenção", "manutencao", "conservação", "conservacao"]):
        return C_FSE_CONSERVACAO
    return C_FSE  # Fornecimentos e serviços de terceiros (genérico)


def construir_lancamento(tipo: str, valor_total, descricao: str = "") -> List[Dict[str, str]]:
    """
    Devolve as linhas de um lançamento EQUILIBRADO (partidas dobradas) usando
    apenas contas do Decreto 82/01. Total débito == total crédito, sempre.

    IVA: se IVA_ATIVO for False (por omissão), lança pelo valor total.
    """
    total = _dec(valor_total)

    if tipo == TIPO_COMPRA_MERCADORIA:
        return _linhas_compra(C_COMPRAS, total, descricao)
    if tipo == TIPO_COMPRA_SERVICO:
        return _linhas_compra(conta_despesa_servico(descricao), total, descricao)
    if tipo == TIPO_VENDA_MERCADORIA:
        return _linhas_venda(C_VENDAS, total, descricao)
    if tipo == TIPO_PRESTACAO_SERVICO:
        return _linhas_venda(C_PRESTACAO_SERVICOS, total, descricao)
    if tipo == TIPO_PAGAMENTO_FORNECEDOR:
        # Débito 32 Fornecedores / Crédito 45 Caixa (ou 43 Banco)
        return [
            _linha(C_FORNECEDORES, debito=total, descricao=descricao),
            _linha(conta_meio_pagamento(descricao), credito=total, descricao=descricao),
        ]
    if tipo == TIPO_RECEBIMENTO_CLIENTE:
        # Débito 45 Caixa (ou 43 Banco) / Crédito 31 Clientes
        return [
            _linha(conta_meio_pagamento(descricao), debito=total, descricao=descricao),
            _linha(C_CLIENTES, credito=total, descricao=descricao),
        ]

    # Não classificado: uma linha a débito e outra a crédito na conta de revisão,
    # equilibrado mas claramente sinalizado como "A_CLASSIFICAR".
    return [
        _linha(CONTA_A_CLASSIFICAR, debito=total, descricao=descricao),
        _linha(CONTA_A_CLASSIFICAR, credito=total, descricao=descricao),
    ]


def _linhas_compra(conta_gasto: tuple, total: Decimal, descricao: str) -> List[Dict[str, str]]:
    """Compra: débito gasto/existência (+IVA dedutível) / crédito Fornecedores (total)."""
    if IVA_ATIVO and CONTA_IVA_DEDUTIVEL and TAXA_IVA:
        base = (total / (Decimal("1") + TAXA_IVA)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
        iva = total - base
        return [
            _linha(conta_gasto, debito=base, descricao=descricao),
            _linha(CONTA_IVA_DEDUTIVEL, debito=iva, descricao="IVA dedutível"),
            _linha(C_FORNECEDORES, credito=total, descricao=descricao),
        ]
    return [
        _linha(conta_gasto, debito=total, descricao=descricao),
        _linha(C_FORNECEDORES, credito=total, descricao=descricao),
    ]


def _linhas_venda(conta_proveito: tuple, total: Decimal, descricao: str) -> List[Dict[str, str]]:
    """Venda: débito Clientes (total) / crédito proveito (+IVA liquidado)."""
    if IVA_ATIVO and CONTA_IVA_LIQUIDADO and TAXA_IVA:
        base = (total / (Decimal("1") + TAXA_IVA)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
        iva = total - base
        return [
            _linha(C_CLIENTES, debito=total, descricao=descricao),
            _linha(conta_proveito, credito=base, descricao=descricao),
            _linha(CONTA_IVA_LIQUIDADO, credito=iva, descricao="IVA liquidado"),
        ]
    return [
        _linha(C_CLIENTES, debito=total, descricao=descricao),
        _linha(conta_proveito, credito=total, descricao=descricao),
    ]


def lancamento_equilibrado(linhas: List[Dict[str, str]]) -> bool:
    """Verificação defensiva: soma dos débitos == soma dos créditos."""
    td = sum(_dec(l.get("debito")) for l in linhas)
    tc = sum(_dec(l.get("credito")) for l in linhas)
    return td == tc