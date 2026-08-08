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


def _dados_fatura(nif: str):
    return {"dados_fatura": {"emitente_nif": nif}}


def test_entidade_nova_sem_perfil_continua_a_chamar_ia():
    analyzer = _criar_analyzer()
    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=RESPOSTA_IA) as ia_mock:
        resultado = analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura("nif-entidade-nova"), "fp-perfil-1")

    ia_mock.assert_called_once()
    assert resultado["modelo"] == "anythingllm:teste"


def test_apos_historico_consistente_perfil_evita_chamada_de_ia():
    analyzer = _criar_analyzer()
    nif = "nif-entidade-consistente"

    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=RESPOSTA_IA) as ia_mock:
        # 3 documentos ambíguos consecutivos desta entidade, todos
        # classificados da mesma forma pela IA (fingerprints diferentes —
        # documentos realmente distintos).
        for i in range(3):
            analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura(nif), f"fp-perfil-consistente-{i}")
        assert ia_mock.call_count == 3

        # 4º documento (novo fingerprint) da MESMA entidade: perfil já
        # estabelecido -> IA não deve ser chamada uma 4ª vez.
        resultado = analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura(nif), "fp-perfil-consistente-4")

    assert ia_mock.call_count == 3
    assert resultado["modelo"] == "perfil_entidade"
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_VENDA_MERCADORIA
    assert resultado["iaCacheHit"] is False


def test_historico_misto_nunca_usa_perfil():
    analyzer = _criar_analyzer()
    nif = "nif-entidade-mista"

    respostas = [
        {**RESPOSTA_IA, "tipo": pgc_ao.TIPO_VENDA_MERCADORIA},
        {**RESPOSTA_IA, "tipo": pgc_ao.TIPO_VENDA_MERCADORIA},
        {**RESPOSTA_IA, "tipo": pgc_ao.TIPO_COMPRA_MERCADORIA},  # tipo diferente
    ]
    with patch.object(analyzer, "_classificar_com_anythingllm", side_effect=respostas) as ia_mock:
        for i in range(3):
            analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura(nif), f"fp-perfil-misto-{i}")

        # histórico misto (2 tipos diferentes) -> nunca decide sozinho,
        # continua a chamar a IA.
        analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura(nif), "fp-perfil-misto-4")

    assert ia_mock.call_count == 4


def test_reanalise_do_mesmo_documento_nao_conta_duas_vezes_no_perfil():
    analyzer = _criar_analyzer()
    nif = "nif-entidade-reanalise"

    with patch.object(analyzer, "_classificar_com_anythingllm", return_value=RESPOSTA_IA) as ia_mock:
        # Reanalisa o MESMO documento (mesmo fingerprint) 5 vezes.
        for _ in range(5):
            analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura(nif), "fp-reanalise-mesmo")

        # Só conta como 1 documento -> perfil nunca atinge o mínimo,
        # continua a chamar a IA (mas usa o cache de IA da Fase 9 a
        # partir da 2ª chamada, por isso _classificar_com_anythingllm
        # só corre mesmo uma vez).
        resultado = analyzer.analyze_document(TEXTO_AMBIGUO, _dados_fatura(nif), "fp-reanalise-outro")

    assert ia_mock.call_count == 2  # 1ª análise (fp-reanalise-mesmo) + documento novo (fp-reanalise-outro)
    assert resultado["modelo"] == "anythingllm:teste"
