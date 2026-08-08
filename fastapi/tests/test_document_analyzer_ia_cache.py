from unittest.mock import patch

from services import pgc as pgc_ao
from services.document_analyzer import DocumentAnalyzer

TEXTO_AMBIGUO = "documento sem nenhuma palavra-chave reconhecida"

RESPOSTA_IA = {
    "tipo": pgc_ao.TIPO_VENDA_MERCADORIA,
    "descricao": "classificado pela IA",
    "confianca": 90,
    "modelo": "anythingllm:teste",
    "fundamentacao": "art. X",
}


def _criar_analyzer() -> DocumentAnalyzer:
    with patch("services.document_analyzer.anythingllm_client.disponivel", return_value=True):
        return DocumentAnalyzer()


def test_segunda_analise_com_mesmo_fingerprint_nao_repete_chamada_de_ia():
    analyzer = _criar_analyzer()
    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=RESPOSTA_IA) as ia_mock:
        r1 = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {}}, "fp-cache-ia-1")
        r2 = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {}}, "fp-cache-ia-1")

    assert ia_mock.call_count == 1
    assert r1["iaCacheHit"] is False
    assert r2["iaCacheHit"] is True
    assert r2["tipoDocumento"] == pgc_ao.TIPO_VENDA_MERCADORIA
    assert r2["fundamentacao"] == "art. X"


def test_fingerprints_diferentes_repetem_chamada_de_ia():
    analyzer = _criar_analyzer()
    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=RESPOSTA_IA) as ia_mock:
        analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {}}, "fp-cache-ia-a")
        analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {}}, "fp-cache-ia-b")

    assert ia_mock.call_count == 2


def test_sem_fingerprint_repete_chamada_de_ia_sempre():
    analyzer = _criar_analyzer()
    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=RESPOSTA_IA) as ia_mock:
        r1 = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {}}, None)
        r2 = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {}}, None)

    assert ia_mock.call_count == 2
    assert r1["iaCacheHit"] is False
    assert r2["iaCacheHit"] is False


def test_documento_inequivoco_por_regras_nao_consulta_cache_de_ia():
    analyzer = _criar_analyzer()
    texto = "Recibo de pagamento recebido do cliente."

    with patch.object(analyzer, "_classificar_com_anythingllm") as ia_mock:
        resultado = analyzer.analyze_document(texto, {"dados_fatura": {}}, "fp-qualquer")

    ia_mock.assert_not_called()
    assert resultado["iaCacheHit"] is False
