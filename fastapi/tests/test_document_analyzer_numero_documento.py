from unittest.mock import patch

from services.document_analyzer import DocumentAnalyzer


def _criar_analyzer() -> DocumentAnalyzer:
    with patch("services.document_analyzer.anythingllm_client.disponivel", return_value=False):
        return DocumentAnalyzer()


def test_numero_documento_extraido_por_regex_e_incluido_na_resposta():
    # Fase 10 do plano de 20 fases ("pesquisa por número; série"):
    # numero_fatura já era extraído por regex_extract.py mas nunca
    # chegava à resposta de /analisar — ficava perdido antes do Java.
    analyzer = _criar_analyzer()
    resultado = analyzer.analyze_document(
        "Fatura de compra de mercadoria.",
        {"dados_fatura": {"numero_fatura": "FT 2026/001", "valor_total_aoa": "1000.00"}},
    )

    assert resultado["numeroDocumento"] == "FT 2026/001"


def test_numero_documento_ausente_fica_none():
    analyzer = _criar_analyzer()
    resultado = analyzer.analyze_document(
        "Fatura de compra de mercadoria.",
        {"dados_fatura": {"valor_total_aoa": "1000.00"}},
    )

    assert resultado["numeroDocumento"] is None
