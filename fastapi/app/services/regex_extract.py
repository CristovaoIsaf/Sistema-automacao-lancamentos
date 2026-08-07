"""
Módulo de extração estruturada de dados de faturas fiscais (padrão AGT - Angola)
a partir de texto já extraído (por OCR ou nativamente do PDF).

Não depende de pdfplumber/pytesseract diretamente — recebe texto puro.
Isso permite reaproveitar a mesma lógica de regex tanto num script standalone
quanto dentro do OCRService da aplicação.
"""

import re
import unicodedata
from dataclasses import dataclass, field
from typing import Optional


# ---------------------------------------------------------------------------
# NORMALIZAÇÃO DE INCONSISTÊNCIAS DE OCR / FONTE
# ---------------------------------------------------------------------------

# Confusões comuns de OCR que atrapalham regex numérico (NIF, valores, datas)
SUBSTITUICOES_NUMERICAS = [
    (r"(?<=\d)[oO](?=\d)", "0"),   # "1o0" -> "100" (O/o entre dígitos)
    (r"(?<=\d)[lI](?=\d)", "1"),   # "1I0" -> "110"
    (r"(?<=\d)[Ss](?=\d)", "5"),   # confusão S/5 em fontes específicas
    (r"(?<=\d)[B](?=\d)", "8"),    # confusão B/8
]


def normalizar_texto(texto: str) -> str:
    """
    Limpa inconsistências típicas de OCR e de diferenças de fonte:
    - normaliza unicode (acentos compostos, espaços não separáveis)
    - colapsa espaços múltiplos e quebras de linha soltas
    - remove caracteres de controle invisíveis

    Usa NFC (não NFKC): NFKC decompõe símbolos como "º" (ordinal) para "o"
    comum, o que corrompe moradas ("nº 45" -> "no 45"). NFC preserva esses
    símbolos e ainda resolve acentos compostos (á = a + acento -> á único).
    """
    texto = unicodedata.normalize("NFC", texto)
    texto = texto.replace("\xa0", " ")
    texto = re.sub(r"[^\S\n]+", " ", texto)
    texto = re.sub(r"\n{2,}", "\n", texto)
    texto = "\n".join(linha.strip() for linha in texto.split("\n"))
    return texto


def normalizar_regiao_numerica(trecho: str) -> str:
    """
    Aplica correções de confusão OCR apenas em trechos que já sabemos ser
    numéricos (ex: já isolamos um candidato a NIF ou valor). Evita aplicar
    globalmente, pois "O" pode ser letra legítima em nomes.
    """
    for padrao, substituto in SUBSTITUICOES_NUMERICAS:
        trecho = re.sub(padrao, substituto, trecho)
    return trecho


# ---------------------------------------------------------------------------
# ESTRUTURA DE DADOS DE SAÍDA
# ---------------------------------------------------------------------------

@dataclass
class DadosFatura:
    emitente_nome: Optional[str] = None
    emitente_nif: Optional[str] = None
    emitente_morada: Optional[str] = None

    adquirente_nome: Optional[str] = None
    adquirente_nif: Optional[str] = None
    adquirente_morada: Optional[str] = None

    numero_fatura: Optional[str] = None
    data_emissao: Optional[str] = None
    hora_emissao: Optional[str] = None

    valor_total_aoa: Optional[str] = None
    taxas_iva_encontradas: list = field(default_factory=list)

    codigo_hash: Optional[str] = None
    software_certificado: Optional[str] = None
    via_original: bool = False

    tipo_documento: Optional[str] = None
    campos_nao_encontrados: list = field(default_factory=list)


# ---------------------------------------------------------------------------
# REGEX DE EXTRAÇÃO
# ---------------------------------------------------------------------------

RE_NIF_ROTULADO = re.compile(r"NIF\s*[:\-]?\s*([A-Z0-9]{9,14})", re.IGNORECASE)

# Fallback: NIF "solto" sem rótulo (10 dígitos consecutivos), usado só se
# a busca rotulada não encontrar nada — menos confiável, por isso é fallback.
RE_NIF_SOLTO = re.compile(r"\b(\d{10})\b")

RE_DATA = re.compile(r"\b(\d{2}[/\-.]\d{2}[/\-.]\d{4})\b")
RE_HORA = re.compile(r"\b(\d{2}:\d{2}(?::\d{2})?)\b")

RE_NUMERO_FATURA = re.compile(
    r"\b(F[TR]|NC)\s*[:\-]?\s*([A-Z0-9]+[/\-][A-Z0-9]+(?:[/\-][A-Z0-9]+)?)",
    re.IGNORECASE,
)

RE_VALOR_AOA = re.compile(
    r"(?:AOA|Kz|Kwanzas?)\s*[:\-]?\s*([\d.,]+)|([\d.,]+)\s*(?:AOA|Kz|Kwanzas?)",
    re.IGNORECASE,
)

RE_TAXA_IVA = re.compile(r"IVA\s*[:\-]?\s*(\d{1,2}(?:[.,]\d{1,2})?)\s*%", re.IGNORECASE)
RE_ISENCAO_IVA = re.compile(
    r"isent[oa]\s+de\s+IVA|n[aã]o\s+sujeit[oa]\s+a\s+IVA|regime\s+de\s+isen[cç][aã]o",
    re.IGNORECASE,
)

RE_HASH = re.compile(r"(?:c[oó]digo\s+)?hash\s*[:\-]?\s*([A-Z0-9]{4,40})", re.IGNORECASE)

RE_SOFTWARE_CERTIFICADO = re.compile(
    r"(?:processado\s+por\s+programa\s+certificado|software\s+certificado)"
    r"\s+n[º°o]?\.?\s*([A-Z0-9./\-]+)",
    re.IGNORECASE,
)

RE_VIA_ORIGINAL = re.compile(r"\boriginal\b", re.IGNORECASE)

TIPOS_DOCUMENTO = {
    "fatura": ["fatura", "factura", "invoice"],
    "fatura_recibo": ["fatura-recibo", "factura-recibo"],
    "recibo": ["recibo", "receipt"],
    "nota_debito": ["nota de débito", "nota de debito"],
    "nota_credito": ["nota de crédito", "nota de credito"],
    "guia_pagamento": ["guia de pagamento"],
    "extrato": ["extrato", "extracto"],
}

ROTULOS_DE_PARADA = [
    "emitente", "adquirente", "cliente", "fornecedor", "nif", "data", "hora",
    "descri", "iva", "total", "hash", "processado por", "original",
    "fatura", "ft ", "fr ", "nc ",
]


def _e_linha_de_novo_rotulo(linha: str) -> bool:
    linha_lower = linha.strip().lower()
    return any(linha_lower.startswith(r) for r in ROTULOS_DE_PARADA)


def _extrair_linhas_do_bloco(texto: str, rotulo: str, linhas_seguintes: int = 3) -> list:
    """
    Retorna as linhas do bloco de um rótulo (ex: 'Emitente:') como LISTA,
    preservando a separação entre nome (1ª linha) e morada (linhas seguintes).
    Para em outro rótulo conhecido ou linha vazia.
    """
    padrao = re.compile(rf"{rotulo}\s*[:\-]?\s*(.+)", re.IGNORECASE)
    linhas = texto.split("\n")

    for idx, linha in enumerate(linhas):
        m = padrao.search(linha)
        if m:
            bloco = [m.group(1).strip()] if m.group(1).strip() else []
            for prox in linhas[idx + 1: idx + 1 + linhas_seguintes]:
                prox = prox.strip()
                if not prox or _e_linha_de_novo_rotulo(prox):
                    break
                bloco.append(prox)
            return bloco
    return []


def _extrair_nome(texto: str, rotulo: str) -> str:
    linhas = _extrair_linhas_do_bloco(texto, rotulo, linhas_seguintes=2)
    return linhas[0].strip() if linhas else ""


def _extrair_morada(texto: str, rotulo: str) -> str:
    linhas = _extrair_linhas_do_bloco(texto, rotulo, linhas_seguintes=2)
    return " ".join(linhas[1:]).strip() if len(linhas) > 1 else ""


def _extrair_nif_por_proximidade(texto: str, rotulo: str, janela_linhas: int = 4) -> Optional[str]:
    """
    Busca o NIF dentro das linhas próximas ao rótulo (Emitente/Adquirente),
    em vez de assumir "1º NIF do documento = emitente, 2º = adquirente".
    Isso é mais robusto a mudanças de layout entre modelos de fatura.
    """
    linhas = texto.split("\n")
    padrao_rotulo = re.compile(rotulo, re.IGNORECASE)

    for idx, linha in enumerate(linhas):
        if padrao_rotulo.search(linha):
            janela = "\n".join(linhas[idx: idx + janela_linhas])
            m = RE_NIF_ROTULADO.search(janela)
            if m:
                return normalizar_regiao_numerica(m.group(1))
    return None


def detectar_tipo_documento(texto_lower: str) -> Optional[str]:
    for tipo, palavras_chave in TIPOS_DOCUMENTO.items():
        if any(kw in texto_lower for kw in palavras_chave):
            return tipo
    return None


def extrair_dados_fatura(texto_bruto: str) -> DadosFatura:
    texto = normalizar_texto(texto_bruto)
    dados = DadosFatura()

    # --- Emitente / Adquirente ---
    # "Fornecedor" é sinónimo comum de "Emitente" em faturas fora do
    # formato estrito AGT (ex: os documentos de teste usados nesta sessão
    # usam "Fornecedor:"/"Cliente:" em vez de "Emitente:"/"Adquirente:").
    dados.emitente_nome = (
        _extrair_nome(texto, "Emitente") or _extrair_nome(texto, "Fornecedor") or None
    )
    dados.emitente_morada = (
        _extrair_morada(texto, "Emitente") or _extrair_morada(texto, "Fornecedor") or None
    )
    dados.emitente_nif = (
        _extrair_nif_por_proximidade(texto, "Emitente")
        or _extrair_nif_por_proximidade(texto, "Fornecedor")
    )

    dados.adquirente_nome = (
        _extrair_nome(texto, "Adquirente") or _extrair_nome(texto, "Cliente") or None
    )
    dados.adquirente_morada = (
        _extrair_morada(texto, "Adquirente") or _extrair_morada(texto, "Cliente") or None
    )
    dados.adquirente_nif = (
        _extrair_nif_por_proximidade(texto, "Adquirente")
        or _extrair_nif_por_proximidade(texto, "Cliente")
    )

    # Fallback: se a busca por proximidade não achou NIFs, usa a ordem de
    # aparição no texto (1º = emitente, 2º = adquirente) como última tentativa.
    if not dados.emitente_nif or not dados.adquirente_nif:
        nifs_ordem = [normalizar_regiao_numerica(n) for n in RE_NIF_ROTULADO.findall(texto)]
        if not dados.emitente_nif and len(nifs_ordem) >= 1:
            dados.emitente_nif = nifs_ordem[0]
        if not dados.adquirente_nif and len(nifs_ordem) >= 2:
            dados.adquirente_nif = nifs_ordem[1]

    # --- Identificação do documento ---
    m_num = RE_NUMERO_FATURA.search(texto)
    if m_num:
        serie_numero = normalizar_regiao_numerica(m_num.group(2))
        dados.numero_fatura = f"{m_num.group(1).upper()} {serie_numero}"

    m_data = RE_DATA.search(texto)
    if m_data:
        dados.data_emissao = normalizar_regiao_numerica(m_data.group(1))

    m_hora = RE_HORA.search(texto)
    if m_hora:
        dados.hora_emissao = m_hora.group(1)

    dados.tipo_documento = detectar_tipo_documento(texto.lower())

    # --- Valores ---
    valores = RE_VALOR_AOA.findall(texto)
    valores_limpos = [v[0] or v[1] for v in valores if v[0] or v[1]]
    if valores_limpos:
        def para_float(v):
            try:
                return float(v.replace(".", "").replace(",", "."))
            except ValueError:
                return -1
        dados.valor_total_aoa = max(valores_limpos, key=para_float)

    dados.taxas_iva_encontradas = RE_TAXA_IVA.findall(texto)
    if RE_ISENCAO_IVA.search(texto):
        dados.taxas_iva_encontradas.append("isento/não sujeito")

    # --- Elementos técnicos AGT ---
    m_hash = RE_HASH.search(texto)
    if m_hash:
        dados.codigo_hash = m_hash.group(1)

    m_software = RE_SOFTWARE_CERTIFICADO.search(texto)
    if m_software:
        dados.software_certificado = m_software.group(1) or "mencionado, sem nº capturado"

    dados.via_original = bool(RE_VIA_ORIGINAL.search(texto))

    # --- Controle de qualidade ---
    campos_obrigatorios = {
        "emitente_nome": dados.emitente_nome,
        "emitente_nif": dados.emitente_nif,
        "adquirente_nif": dados.adquirente_nif,
        "numero_fatura": dados.numero_fatura,
        "data_emissao": dados.data_emissao,
        "valor_total_aoa": dados.valor_total_aoa,
        "codigo_hash": dados.codigo_hash,
    }
    dados.campos_nao_encontrados = [c for c, v in campos_obrigatorios.items() if not v]

    return dados


# ---------------------------------------------------------------------------
# VALIDAÇÃO PÓS-EXTRAÇÃO (qualidade de dados)
# ---------------------------------------------------------------------------

def validar_nif_formato(nif: Optional[str]) -> bool:
    """Validação básica de formato (comprimento). Não valida dígito verificador."""
    if not nif:
        return False
    return 9 <= len(nif) <= 14 and nif.isalnum()


def gerar_relatorio_qualidade(dados: DadosFatura) -> dict:
    return {
        "campos_faltantes": dados.campos_nao_encontrados,
        "nif_emitente_formato_valido": validar_nif_formato(dados.emitente_nif),
        "nif_adquirente_formato_valido": validar_nif_formato(dados.adquirente_nif),
        "confianca_geral": (
            "alta" if not dados.campos_nao_encontrados
            else "media" if len(dados.campos_nao_encontrados) <= 2
            else "baixa"
        ),
    }