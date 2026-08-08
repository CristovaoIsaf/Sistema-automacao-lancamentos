"""
Motor de validação determinística (Fase 3 do mapa de impacto) — corre
ANTES de qualquer classificação por IA (AnythingLLM) e decide tudo o que
puder ser decidido por regras de código, sem depender de nenhum modelo.

Cada responsabilidade de validação é uma função isolada e testável, para
não ficarem espalhadas por controllers/componentes React nem duplicadas
entre camadas — este módulo é a fonte única de verdade para "este
documento está bem formado?".

IMPORTANTE: este motor valida a QUALIDADE/CONSISTÊNCIA dos dados já
extraídos por regex_extract.extrair_dados_fatura (campos presentes,
formatos, datas, valores). NÃO decide o tipo de OPERAÇÃO contabilística
(compra/venda/prestação/...) — essa é uma classificação diferente,
continua a cargo do DocumentAnalyzer (regras ou IA). São duas
classificações independentes: tipo de DOCUMENTO (fatura/recibo/nota...)
vs tipo de OPERAÇÃO (compra_mercadoria/venda_mercadoria/...).
"""

from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from decimal import Decimal, InvalidOperation
from typing import Callable, List, Optional

from services.cache_versionado import CacheVersionado
from services.pgc import TAXAS_IVA_RECONHECIDAS
from services.regex_extract import DadosFatura, validar_nif_formato

# Versão deste motor — incrementar sempre que uma regra mudar de
# comportamento, para que um resultado de validação antigo nunca seja
# confundido com um calculado por um conjunto de regras diferente (mesmo
# padrão de DOCUMENT_REQUIREMENTS_VERSION em document_requirements.py).
VALIDATION_ENGINE_VERSION = "TFC-2026-v1"

# Sanidade de data: sem limite superior de antiguidade rígido não há forma
# de distinguir um documento histórico legítimo de um erro de OCR no ano
# (ex: "2016" em vez de "2026") — sinalizado como AVISO, não erro, porque
# pode ser legítimo. Futuro tem tolerância mínima (relógio do scanner).
ANOS_MAX_ANTIGUIDADE = 10
DIAS_TOLERANCIA_FUTURO = 1


@dataclass
class ProblemaValidacao:
    campo: str
    codigo: str
    mensagem: str
    gravidade: str  # "erro" | "aviso"


@dataclass
class ResultadoValidacao:
    problemas: List[ProblemaValidacao] = field(default_factory=list)
    versao: str = VALIDATION_ENGINE_VERSION

    @property
    def valido(self) -> bool:
        return not any(p.gravidade == "erro" for p in self.problemas)

    def to_dict(self) -> dict:
        return {
            "valido": self.valido,
            "versao": self.versao,
            "problemas": [
                {
                    "campo": p.campo,
                    "codigo": p.codigo,
                    "mensagem": p.mensagem,
                    "gravidade": p.gravidade,
                }
                for p in self.problemas
            ],
        }


def _erro(campo: str, codigo: str, mensagem: str) -> ProblemaValidacao:
    return ProblemaValidacao(campo, codigo, mensagem, "erro")


def _aviso(campo: str, codigo: str, mensagem: str) -> ProblemaValidacao:
    return ProblemaValidacao(campo, codigo, mensagem, "aviso")


# ---------------------------------------------------------------------------
# REGRAS — cada uma cobre uma única responsabilidade (ver docstring do
# módulo). Todas recebem DadosFatura e devolvem a lista de problemas que
# encontraram (lista vazia = nada a assinalar).
# ---------------------------------------------------------------------------

def regra_campos_obrigatorios(dados: DadosFatura) -> List[ProblemaValidacao]:
    """Reaproveita dados.campos_nao_encontrados, já calculado em
    extrair_dados_fatura a partir de document_requirements.requisitos_para
    — não repete aqui a matriz de requisitos (fonte única, ver secção 6 do
    mapa de impacto: não duplicar regras entre camadas/ficheiros)."""
    return [
        _erro(
            campo,
            "campo_obrigatorio_ausente",
            f"Campo obrigatório '{campo}' não encontrado no documento.",
        )
        for campo in dados.campos_nao_encontrados
    ]


def regra_formato_nif(dados: DadosFatura) -> List[ProblemaValidacao]:
    problemas = []
    if dados.emitente_nif and not validar_nif_formato(dados.emitente_nif):
        problemas.append(_erro(
            "emitente_nif", "nif_formato_invalido",
            f"NIF do emitente '{dados.emitente_nif}' tem formato inválido.",
        ))
    if dados.adquirente_nif and not validar_nif_formato(dados.adquirente_nif):
        problemas.append(_erro(
            "adquirente_nif", "nif_formato_invalido",
            f"NIF do adquirente '{dados.adquirente_nif}' tem formato inválido.",
        ))
    return problemas


def regra_identificacao_partes(dados: DadosFatura) -> List[ProblemaValidacao]:
    """Consistência entre campos: emitente e adquirente não podem
    partilhar o mesmo NIF — indica erro de extração (proximidade apanhou o
    mesmo NIF duas vezes) ou documento inconsistente."""
    if (
        dados.emitente_nif
        and dados.adquirente_nif
        and dados.emitente_nif == dados.adquirente_nif
    ):
        return [_erro(
            "adquirente_nif", "emitente_adquirente_mesmo_nif",
            "Emitente e adquirente têm o mesmo NIF — verifique o documento.",
        )]
    return []


def _parse_data(texto: str) -> Optional[date]:
    for separador in ("/", "-", "."):
        partes = texto.split(separador)
        if len(partes) == 3:
            try:
                dia, mes, ano = (int(p) for p in partes)
                return date(ano, mes, dia)
            except ValueError:
                return None
    return None


def regra_data_emissao(dados: DadosFatura) -> List[ProblemaValidacao]:
    if not dados.data_emissao:
        return []  # ausência já coberta por regra_campos_obrigatorios quando aplicável

    data = _parse_data(dados.data_emissao)
    if data is None:
        return [_erro(
            "data_emissao", "data_formato_invalido",
            f"Data de emissão '{dados.data_emissao}' não é uma data válida.",
        )]

    hoje = datetime.now().date()
    if data > hoje + timedelta(days=DIAS_TOLERANCIA_FUTURO):
        return [_erro(
            "data_emissao", "data_futura",
            f"Data de emissão '{dados.data_emissao}' está no futuro.",
        )]
    if data.year <= hoje.year - ANOS_MAX_ANTIGUIDADE:
        return [_aviso(
            "data_emissao", "data_muito_antiga",
            f"Data de emissão '{dados.data_emissao}' tem mais de "
            f"{ANOS_MAX_ANTIGUIDADE} anos — confirme se o OCR não leu o ano errado.",
        )]
    return []


def _parse_valor(texto: str) -> Optional[Decimal]:
    try:
        return Decimal(texto.replace(".", "").replace(",", "."))
    except (InvalidOperation, AttributeError):
        return None


def regra_valor_total(dados: DadosFatura) -> List[ProblemaValidacao]:
    if not dados.valor_total_aoa:
        return []  # ausência já coberta por regra_campos_obrigatorios quando aplicável

    valor = _parse_valor(dados.valor_total_aoa)
    if valor is None:
        return [_erro(
            "valor_total_aoa", "valor_total_formato_invalido",
            f"Valor total '{dados.valor_total_aoa}' não é um número válido.",
        )]
    if valor <= 0:
        return [_erro(
            "valor_total_aoa", "valor_total_nao_positivo",
            f"Valor total '{dados.valor_total_aoa}' deve ser maior que zero.",
        )]
    return []


def regra_taxa_iva_reconhecida(dados: DadosFatura) -> List[ProblemaValidacao]:
    """Avisa (não bloqueia) quando o documento indica uma taxa de IVA que
    este projeto não trata (ver pgc.TAXAS_IVA_RECONHECIDAS — fonte única,
    só 14%/7% estão em vigor). O lançamento continua a ser gerado com a
    taxa geral por omissão (pgc._resolver_taxa_iva), mas o utilizador deve
    poder ver que a taxa indicada no documento foi ignorada."""
    problemas = []
    for taxa in dados.taxas_iva_encontradas:
        taxa_normalizada = str(taxa).replace(",", ".").split(".")[0]
        if taxa_normalizada == "isento/não sujeito":
            continue
        if taxa_normalizada not in TAXAS_IVA_RECONHECIDAS:
            problemas.append(_aviso(
                "taxas_iva_encontradas", "taxa_iva_nao_reconhecida",
                f"Taxa de IVA '{taxa}' no documento não é reconhecida (só 14% "
                "ou 7%) — será usada a taxa geral por omissão.",
            ))
    return problemas


def regra_tipo_documento_identificado(dados: DadosFatura) -> List[ProblemaValidacao]:
    if not dados.tipo_documento:
        return [_aviso(
            "tipo_documento", "tipo_documento_nao_identificado",
            "Não foi possível identificar o tipo de documento (fatura, "
            "recibo, ...) pelo texto — a validar como fatura (conjunto "
            "de requisitos mais rigoroso).",
        )]
    return []


REGRAS: List[Callable[[DadosFatura], List[ProblemaValidacao]]] = [
    regra_campos_obrigatorios,
    regra_formato_nif,
    regra_identificacao_partes,
    regra_data_emissao,
    regra_valor_total,
    regra_taxa_iva_reconhecida,
    regra_tipo_documento_identificado,
]


def validar_documento(dados: DadosFatura) -> ResultadoValidacao:
    """Corre todas as regras determinísticas e agrega os problemas
    encontrados. Puramente determinístico — sem I/O, sem chamada a IA."""
    problemas: List[ProblemaValidacao] = []
    for regra in REGRAS:
        problemas.extend(regra(dados))
    return ResultadoValidacao(problemas=problemas)


# Fase 6 do mapa de impacto — evita recalcular a validação para o mesmo
# documento (mesmo fingerprint) já visto antes (ex: reanálise). Indexado
# também por VALIDATION_ENGINE_VERSION (ver CacheVersionado): se as regras
# mudarem, uma entrada antiga nunca é confundida com uma calculada pelas
# regras actuais.
_cache_validacao: CacheVersionado = CacheVersionado("validacao")


def validar_documento_com_cache(dados: DadosFatura, fingerprint: Optional[str]) -> ResultadoValidacao:
    """Variante de validar_documento que reaproveita o resultado do cache
    quando `fingerprint` já foi validado antes com a versão actual das
    regras. Sem fingerprint, calcula sempre de novo — não há chave de
    cache válida (chamador fora do fluxo normal de análise)."""
    if not fingerprint:
        return validar_documento(dados)

    resultado, _ = _cache_validacao.obter_ou_calcular(
        fingerprint, VALIDATION_ENGINE_VERSION, lambda: validar_documento(dados)
    )
    return resultado
