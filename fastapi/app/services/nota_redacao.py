"""
Fase 14 do plano de 20 fases — "Notas às Contas": redação assistida
(opcional) do texto explicativo de uma nota, a partir dos dados JÁ
CALCULADOS pelo backend Java (ver NotaContaService/NotaContaController) —
este módulo nunca calcula nem reinterpreta valores, só os formata em
prosa. "Toda informação numérica deve ter origem rastreável nos dados
contabilísticos": os únicos números que podem aparecer no texto final são
os que já vêm no pedido.

Duas fontes possíveis, devolvidas em `fonte`:
  - "template": frase determinística, gerada por interpolação directa dos
    números recebidos — sempre disponível, zero risco de inventar dados.
  - "ia": pede ao AnythingLLM para reescrever o MESMO conteúdo em prosa
    mais natural — nunca gera factos novos, só reescreve; a resposta é
    validada a seguir (todo número que apareça no texto tem de já
    existir nos dados de origem) antes de ser aceite. Falha em qualquer
    passo (IA indisponível, resposta vazia, número desconhecido) cai
    sempre de volta ao template — nunca falha o pedido inteiro.
"""

import logging
import re
from typing import Optional

from pydantic import BaseModel

from config.settings import settings
from services import anythingllm_client

logger = logging.getLogger(__name__)


class GrupoEntidadeRedacaoDTO(BaseModel):
    entidade: str
    tipo: Optional[str] = None
    totalDebito: str = "0.00"
    totalCredito: str = "0.00"


class NotaRedacaoRequest(BaseModel):
    conta: str
    nomeConta: Optional[str] = None
    # DEVEDORA / CREDORA (ver plano de contas, Fase 6) — usada só para
    # decidir se o saldo se lê como "devedor" ou "credor"; nunca inventada
    # aqui quando ausente (cai no critério pelo sinal do saldo).
    natureza: Optional[str] = None
    inicio: Optional[str] = None
    fim: Optional[str] = None
    totalDebito: str = "0.00"
    totalCredito: str = "0.00"
    saldo: str = "0.00"
    porEntidade: list[GrupoEntidadeRedacaoDTO] = []


def _formatar_valor(valor: str) -> str:
    return f"{valor} AOA"


def _saldo_numero(saldo: str) -> float:
    try:
        return float(str(saldo).replace(",", "."))
    except ValueError:
        return 0.0


def _tipo_de_saldo(natureza: Optional[str], saldo_num: float) -> str:
    if natureza == "CREDORA":
        return "credor" if saldo_num <= 0 else "devedor"
    # DEVEDORA ou desconhecida: por omissão lê-se pelo sinal do saldo
    # (mesmo critério já usado em BalanceteService.construirLinha).
    return "devedor" if saldo_num >= 0 else "credor"


def redigir_template(dados: NotaRedacaoRequest) -> str:
    nome_conta = dados.nomeConta or dados.conta
    periodo_txt = f"entre {dados.inicio} e {dados.fim}" if dados.inicio and dados.fim else "no período consultado"
    saldo_num = _saldo_numero(dados.saldo)
    tipo_saldo = _tipo_de_saldo(dados.natureza, saldo_num)

    partes = [
        f"A conta {dados.conta} — {nome_conta} registou, {periodo_txt}, um total de débitos de "
        f"{_formatar_valor(dados.totalDebito)} e um total de créditos de {_formatar_valor(dados.totalCredito)}, "
        f"resultando num saldo {tipo_saldo} de {_formatar_valor(f'{abs(saldo_num):.2f}')}."
    ]

    if dados.porEntidade:
        descricoes = [
            f"{g.entidade} (débito {_formatar_valor(g.totalDebito)}, crédito {_formatar_valor(g.totalCredito)})"
            for g in dados.porEntidade
        ]
        plural = "entidade" if len(dados.porEntidade) == 1 else "entidades"
        partes.append(
            f"Este saldo é composto por movimentos de {len(dados.porEntidade)} {plural}: "
            + "; ".join(descricoes) + "."
        )
    else:
        partes.append("Não foram registados movimentos nesta conta no período consultado.")

    return " ".join(partes)


# Números com separador decimal (formato em que todo o valor monetário
# chega, ver pgc.py::_dec) ou com 3+ dígitos — evita rejeitar à toa por
# causa de anos ("2026") ou contagens curtas ("1 entidade").
_RE_NUMERO = re.compile(r"\d[\d.,]*\d|\d")


def _numeros_conhecidos(dados: NotaRedacaoRequest) -> set[str]:
    conhecidos: set[str] = set()
    for valor in (dados.totalDebito, dados.totalCredito, dados.saldo):
        conhecidos.update(_RE_NUMERO.findall(valor))
    # inicio/fim (ex. "2026-01-01") também são dados de origem legítimos —
    # sem isto, a IA mencionar o ano ("no exercício de 2026") era
    # incorretamente tratado como um valor inventado.
    for data in (dados.inicio, dados.fim):
        if data:
            conhecidos.update(_RE_NUMERO.findall(data))
    for g in dados.porEntidade:
        conhecidos.update(_RE_NUMERO.findall(g.totalDebito))
        conhecidos.update(_RE_NUMERO.findall(g.totalCredito))
    return conhecidos


def _texto_so_usa_numeros_conhecidos(texto: str, dados: NotaRedacaoRequest) -> bool:
    numeros_no_texto = set(_RE_NUMERO.findall(texto))
    numeros_conhecidos = _numeros_conhecidos(dados)
    suspeitos = {n for n in numeros_no_texto if ("," in n or "." in n or len(n) >= 3)}
    return suspeitos.issubset(numeros_conhecidos)


def redigir_com_ia(dados: NotaRedacaoRequest, texto_template: str) -> Optional[str]:
    """Devolve o texto reescrito pela IA, ou None se a IA não estiver
    disponível ou a resposta falhar a validação — nunca arrisca devolver
    um valor inventado."""
    if not anythingllm_client.disponivel():
        return None

    prompt = (
        "Reescreve o texto abaixo em prosa fluida e profissional, adequada a uma "
        "nota explicativa de demonstrações financeiras em português de Angola. "
        "NÃO inventes nem calcules nenhum valor novo — usa exactamente os números "
        "já presentes no texto, sem os alterar. Não acrescentes informação que não "
        "esteja já aqui.\n\n" + texto_template
    )

    try:
        resultado = anythingllm_client.consultar_workspace(prompt, settings.ANYTHINGLLM_WORKSPACE_EXEMPLOS)
    except anythingllm_client.AnythingLLMError as e:
        logger.warning("Não foi possível gerar a redação assistida por IA: %s", e)
        return None

    texto = (resultado.get("resposta") or "").strip()
    if not texto:
        return None

    if not _texto_so_usa_numeros_conhecidos(texto, dados):
        logger.warning("Redação da IA contém números fora dos dados de origem — a usar o template.")
        return None

    return texto


def redigir(dados: NotaRedacaoRequest) -> dict:
    texto_template = redigir_template(dados)
    texto_ia = redigir_com_ia(dados, texto_template)
    if texto_ia:
        return {"texto": texto_ia, "fonte": "ia"}
    return {"texto": texto_template, "fonte": "template"}
