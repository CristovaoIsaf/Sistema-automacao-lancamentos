from unittest.mock import patch

from services import pgc as pgc_ao
from services.document_analyzer import DocumentAnalyzer

# Texto de compra GENÉRICA — sem "mercadoria" nem palavras de serviço no
# PRÓPRIO documento, para isolar o efeito do contexto da empresa.
TEXTO_COMPRA_GENERICA = "Fatura emitida por um fornecedor qualquer."

CONTEXTO_EMPRESA_SERVICOS = {"atividade_economica": "", "natureza_negocio": "Prestação de serviços de consultoria"}
CONTEXTO_EMPRESA_REVENDA = {"atividade_economica": "Comércio a retalho de veículos automóveis", "natureza_negocio": ""}


def _criar_analyzer() -> DocumentAnalyzer:
    with patch("services.document_analyzer.anythingllm_client.disponivel", return_value=False):
        return DocumentAnalyzer()


def test_compra_generica_sem_contexto_mantem_compra_mercadoria():
    analyzer = _criar_analyzer()
    resultado = analyzer.analyze_document(TEXTO_COMPRA_GENERICA, {"dados_fatura": {}})
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_COMPRA_MERCADORIA


def test_compra_generica_com_empresa_prestadora_de_servicos_vira_compra_servico():
    analyzer = _criar_analyzer()
    resultado = analyzer.analyze_document(
        TEXTO_COMPRA_GENERICA, {"dados_fatura": {}}, contexto_empresa=CONTEXTO_EMPRESA_SERVICOS
    )
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_COMPRA_SERVICO


def test_compra_generica_com_empresa_de_revenda_mantem_compra_mercadoria():
    analyzer = _criar_analyzer()
    resultado = analyzer.analyze_document(
        TEXTO_COMPRA_GENERICA, {"dados_fatura": {}}, contexto_empresa=CONTEXTO_EMPRESA_REVENDA
    )
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_COMPRA_MERCADORIA


def test_mercadoria_explicita_no_documento_ignora_contexto_da_empresa():
    # O documento é mais específico do que o contexto da empresa — nunca
    # deve ser ignorado só porque a empresa "normalmente" presta serviços.
    analyzer = _criar_analyzer()
    resultado = analyzer.analyze_document(
        "Fatura de compra de mercadoria para o armazém.",
        {"dados_fatura": {}},
        contexto_empresa=CONTEXTO_EMPRESA_SERVICOS,
    )
    assert resultado["tipoDocumento"] == pgc_ao.TIPO_COMPRA_MERCADORIA


def test_prompt_inclui_contexto_da_empresa_quando_fornecido():
    analyzer = _criar_analyzer()
    prompt = analyzer._construir_prompt_classificacao(
        "texto", {}, contexto_empresa={"atividade_economica": "Construção civil", "natureza_negocio": "Empreiteiro"}
    )
    assert "Construção civil" in prompt
    assert "Empreiteiro" in prompt


def test_prompt_sem_contexto_da_empresa_nao_inclui_seccao():
    analyzer = _criar_analyzer()
    prompt = analyzer._construir_prompt_classificacao("texto", {}, contexto_empresa=None)
    assert "CONTEXTO DA EMPRESA" not in prompt
