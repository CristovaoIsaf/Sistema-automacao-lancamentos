from unittest.mock import patch

from services import document_validation
from services.document_validation import validar_documento, validar_documento_com_cache
from services.regex_extract import DadosFatura, extrair_dados_fatura


def _fatura_valida(**overrides) -> DadosFatura:
    """DadosFatura de uma factura completa e coerente — usada como base
    pelos testes que só querem violar UMA regra de cada vez."""
    dados = DadosFatura(
        emitente_nome="Sonangol Distribuidora Lda",
        emitente_nif="123456789",
        adquirente_nome="Cliente Teste Lda",
        adquirente_nif="987654321",
        numero_fatura="FT 2026/001",
        data_emissao="15/03/2024",
        valor_total_aoa="150.000,00",
        taxas_iva_encontradas=["14"],
        codigo_hash="ABC123DEF456",
        tipo_documento="fatura",
        campos_nao_encontrados=[],
    )
    for campo, valor in overrides.items():
        setattr(dados, campo, valor)
    return dados


# ── Documento válido / inválido / incompleto (ponta a ponta) ───────────────

def test_documento_valido_nao_tem_erros():
    resultado = validar_documento(_fatura_valida())
    assert resultado.valido is True
    assert resultado.problemas == []


def test_documento_incompleto_fica_invalido():
    dados = _fatura_valida(codigo_hash=None, campos_nao_encontrados=["codigo_hash"])
    resultado = validar_documento(dados)
    assert resultado.valido is False
    codigos = [p.codigo for p in resultado.problemas]
    assert "campo_obrigatorio_ausente" in codigos


def test_documento_valido_extraido_de_texto_real():
    texto = """FATURA
Emitente: Sonangol Distribuidora Lda
NIF: 123456789

Adquirente: Cliente Teste Lda
NIF: 987654321

Fatura: FT 2026/001
Data: 15/03/2024

Total: 150.000,00 AOA
IVA: 14%

Hash: ABC123DEF456
"""
    dados = extrair_dados_fatura(texto)
    resultado = validar_documento(dados)
    assert resultado.valido is True, resultado.to_dict()


# ── Cada regra isolada ──────────────────────────────────────────────────

def test_nif_com_formato_invalido_e_erro():
    resultado = validar_documento(_fatura_valida(emitente_nif="123"))
    codigos = [p.codigo for p in resultado.problemas]
    assert "nif_formato_invalido" in codigos
    assert resultado.valido is False


def test_emitente_e_adquirente_com_mesmo_nif_e_erro():
    resultado = validar_documento(_fatura_valida(adquirente_nif="123456789"))
    codigos = [p.codigo for p in resultado.problemas]
    assert "emitente_adquirente_mesmo_nif" in codigos
    assert resultado.valido is False


def test_data_em_formato_invalido_e_erro():
    resultado = validar_documento(_fatura_valida(data_emissao="32/13/2024"))
    codigos = [p.codigo for p in resultado.problemas]
    assert "data_formato_invalido" in codigos
    assert resultado.valido is False


def test_data_no_futuro_e_erro():
    resultado = validar_documento(_fatura_valida(data_emissao="01/01/2099"))
    codigos = [p.codigo for p in resultado.problemas]
    assert "data_futura" in codigos
    assert resultado.valido is False


def test_data_muito_antiga_e_apenas_aviso():
    resultado = validar_documento(_fatura_valida(data_emissao="01/01/2000"))
    problema = next(p for p in resultado.problemas if p.codigo == "data_muito_antiga")
    assert problema.gravidade == "aviso"
    # Um aviso sozinho não invalida o documento.
    assert resultado.valido is True


def test_valor_total_nao_numerico_e_erro():
    resultado = validar_documento(_fatura_valida(valor_total_aoa="abc"))
    codigos = [p.codigo for p in resultado.problemas]
    assert "valor_total_formato_invalido" in codigos
    assert resultado.valido is False


def test_valor_total_zero_e_erro():
    resultado = validar_documento(_fatura_valida(valor_total_aoa="0,00"))
    codigos = [p.codigo for p in resultado.problemas]
    assert "valor_total_nao_positivo" in codigos
    assert resultado.valido is False


def test_taxa_iva_nao_reconhecida_e_apenas_aviso():
    resultado = validar_documento(_fatura_valida(taxas_iva_encontradas=["23"]))
    problema = next(p for p in resultado.problemas if p.codigo == "taxa_iva_nao_reconhecida")
    assert problema.gravidade == "aviso"
    assert resultado.valido is True


def test_taxa_iva_isenta_nao_gera_problema():
    resultado = validar_documento(_fatura_valida(taxas_iva_encontradas=["isento/não sujeito"]))
    codigos = [p.codigo for p in resultado.problemas]
    assert "taxa_iva_nao_reconhecida" not in codigos


def test_tipo_documento_nao_identificado_e_apenas_aviso():
    resultado = validar_documento(_fatura_valida(tipo_documento=None))
    problema = next(p for p in resultado.problemas if p.codigo == "tipo_documento_nao_identificado")
    assert problema.gravidade == "aviso"
    assert resultado.valido is True


def test_to_dict_tem_forma_estavel():
    resultado = validar_documento(_fatura_valida(emitente_nif="123"))
    payload = resultado.to_dict()
    assert set(payload.keys()) == {"valido", "versao", "problemas"}
    assert payload["problemas"][0].keys() == {"campo", "codigo", "mensagem", "gravidade"}


# ── Cache de validação (Fase 6) ─────────────────────────────────────────

def test_com_cache_segunda_chamada_com_mesmo_fingerprint_nao_recalcula():
    with patch.object(
        document_validation, "validar_documento", wraps=document_validation.validar_documento
    ) as espia:
        r1 = validar_documento_com_cache(_fatura_valida(), "fp-cache-1")
        r2 = validar_documento_com_cache(_fatura_valida(), "fp-cache-1")

    assert espia.call_count == 1
    assert r1.valido is True
    assert r2.valido is True


def test_com_cache_sem_fingerprint_recalcula_sempre():
    with patch.object(
        document_validation, "validar_documento", wraps=document_validation.validar_documento
    ) as espia:
        validar_documento_com_cache(_fatura_valida(), None)
        validar_documento_com_cache(_fatura_valida(), None)

    assert espia.call_count == 2


def test_mudanca_de_versao_do_motor_forca_recalculo():
    """Fase 13 — item 9: usa a constante REAL VALIDATION_ENGINE_VERSION
    (não uma string arbitrária) para confirmar que uma mudança na versão
    do motor de validação invalida um resultado já em cache — não é só o
    mecanismo genérico de cache_versionado.py, é esta ligação real."""
    dados = _fatura_valida()
    fingerprint = "fp-versao-motor-real"

    validar_documento_com_cache(dados, fingerprint)  # popula o cache com a versão actual

    with patch.object(document_validation, "VALIDATION_ENGINE_VERSION", "TFC-2026-vTESTE"):
        with patch.object(
            document_validation, "validar_documento", wraps=document_validation.validar_documento
        ) as espia:
            resultado = validar_documento_com_cache(dados, fingerprint)

    espia.assert_called_once()  # não reaproveitou o cache da versão antiga
    assert resultado.valido is True
