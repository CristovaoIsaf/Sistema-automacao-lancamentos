from unittest.mock import patch

from services import pgc as pgc_ao
from services.document_analyzer import DocumentAnalyzer


def _criar_analyzer(use_ai: bool) -> DocumentAnalyzer:
    with patch("services.document_analyzer.anythingllm_client.disponivel", return_value=use_ai):
        return DocumentAnalyzer()


# ── _precisa_de_ia isolada ──────────────────────────────────────────────

def test_precisa_de_ia_falso_quando_regras_ja_classificaram():
    analyzer = _criar_analyzer(use_ai=False)
    assert analyzer._precisa_de_ia({"tipo": pgc_ao.TIPO_VENDA_MERCADORIA}) is False


def test_precisa_de_ia_verdadeiro_quando_regras_nao_decidiram():
    analyzer = _criar_analyzer(use_ai=False)
    assert analyzer._precisa_de_ia({"tipo": pgc_ao.TIPO_A_CLASSIFICAR}) is True


# ── analyze_document — o gate ponta a ponta ─────────────────────────────

def test_documento_inequivoco_por_regras_nao_chama_ia_mesmo_disponivel():
    analyzer = _criar_analyzer(use_ai=True)
    texto = "Recibo de pagamento recebido do cliente."

    with patch.object(analyzer, "_classificar_com_anythingllm") as ia_mock:
        resultado = analyzer.analyze_document(texto, {"dados_fatura": {}})

    ia_mock.assert_not_called()
    assert resultado["modelo"] == "regras"
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_RECEBIMENTO_CLIENTE


def test_documento_ambiguo_com_ia_disponivel_chama_ia():
    analyzer = _criar_analyzer(use_ai=True)
    texto = "documento sem nenhuma palavra-chave reconhecida"

    resposta_ia = {
        "tipo": pgc_ao.TIPO_VENDA_MERCADORIA,
        "descricao": "classificado pela IA",
        "confianca": 90,
        "modelo": "anythingllm:teste",
        "fundamentacao": "art. X",
    }
    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=resposta_ia) as ia_mock:
        resultado = analyzer.analyze_document(texto, {"dados_fatura": {}})

    ia_mock.assert_called_once()
    assert resultado["modelo"] == "anythingllm:teste"
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_VENDA_MERCADORIA


def test_documento_ambiguo_sem_ia_disponivel_usa_regras_sem_chamar_ia():
    analyzer = _criar_analyzer(use_ai=False)
    texto = "documento sem nenhuma palavra-chave reconhecida"

    with patch.object(analyzer, "_classificar_com_anythingllm") as ia_mock:
        resultado = analyzer.analyze_document(texto, {"dados_fatura": {}})

    ia_mock.assert_not_called()
    assert resultado["modelo"] == "regras"
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_A_CLASSIFICAR


def test_erro_na_ia_recorre_a_regras_sem_rebentar():
    analyzer = _criar_analyzer(use_ai=True)
    texto = "documento sem nenhuma palavra-chave reconhecida"

    with patch.object(analyzer, "_classificar_com_anythingllm", side_effect=RuntimeError("falhou")):
        resultado = analyzer.analyze_document(texto, {"dados_fatura": {}})

    assert resultado["modelo"] == "regras"
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_A_CLASSIFICAR
