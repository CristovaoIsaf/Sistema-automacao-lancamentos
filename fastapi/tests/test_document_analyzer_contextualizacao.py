from unittest.mock import patch

from services import pgc as pgc_ao
from services.document_analyzer import DocumentAnalyzer

TEXTO_AMBIGUO = "documento sem nenhuma palavra-chave reconhecida"
TEXTO_INEQUIVOCO = "Recibo de pagamento recebido do cliente."


def _criar_analyzer(use_ai: bool = False) -> DocumentAnalyzer:
    with patch("services.document_analyzer.anythingllm_client.disponivel", return_value=use_ai):
        return DocumentAnalyzer()


def test_documento_ambiguo_sem_ia_disponivel_gera_pergunta_contextualizacao():
    # Fase 4 — sem IA disponível e sem regras/perfil a decidir, o sistema
    # cai em a_classificar e deve pedir contextualização em vez de
    # simplesmente desistir.
    analyzer = _criar_analyzer(use_ai=False)
    resultado = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {"valor_total_aoa": "1000.00"}})

    assert resultado["tipoDocumento"] == pgc_ao.TIPO_A_CLASSIFICAR
    pergunta = resultado["perguntaContextualizacao"]
    assert pergunta is not None
    assert pergunta["pergunta"]
    valores = [o["valor"] for o in pergunta["opcoes"]]
    assert valores == ["mercadoria_revenda", "servico", "outro"]


def test_documento_inequivoco_nao_gera_pergunta_contextualizacao():
    analyzer = _criar_analyzer(use_ai=False)
    resultado = analyzer.analyze_document(TEXTO_INEQUIVOCO, {"dados_fatura": {}})

    assert resultado["tipoDocumento"] != pgc_ao.TIPO_A_CLASSIFICAR
    assert resultado["perguntaContextualizacao"] is None


def test_opcao_mercadoria_tem_linhas_equilibradas_com_conta_de_compras():
    analyzer = _criar_analyzer(use_ai=False)
    resultado = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {"valor_total_aoa": "1000.00"}})

    opcao = next(o for o in resultado["perguntaContextualizacao"]["opcoes"] if o["valor"] == "mercadoria_revenda")
    assert opcao["tipo"] == pgc_ao.TIPO_COMPRA_MERCADORIA
    assert pgc_ao.lancamento_equilibrado(opcao["linhas"])
    contas = [l["conta"] for l in opcao["linhas"]]
    assert pgc_ao.C_COMPRAS[0] in contas
    assert pgc_ao.C_FORNECEDORES[0] in contas


def test_opcao_servico_tem_linhas_equilibradas():
    analyzer = _criar_analyzer(use_ai=False)
    resultado = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {"valor_total_aoa": "1000.00"}})

    opcao = next(o for o in resultado["perguntaContextualizacao"]["opcoes"] if o["valor"] == "servico")
    assert opcao["tipo"] == pgc_ao.TIPO_COMPRA_SERVICO
    assert pgc_ao.lancamento_equilibrado(opcao["linhas"])


def test_opcao_outro_fica_a_classificar_sem_inventar_conta():
    # Nunca oferecer "ativo"/"equipamento administrativo" com uma conta
    # inventada — "outro" fica honestamente a_classificar.
    analyzer = _criar_analyzer(use_ai=False)
    resultado = analyzer.analyze_document(TEXTO_AMBIGUO, {"dados_fatura": {"valor_total_aoa": "1000.00"}})

    opcao = next(o for o in resultado["perguntaContextualizacao"]["opcoes"] if o["valor"] == "outro")
    assert opcao["tipo"] == pgc_ao.TIPO_A_CLASSIFICAR
    assert pgc_ao.lancamento_equilibrado(opcao["linhas"])
