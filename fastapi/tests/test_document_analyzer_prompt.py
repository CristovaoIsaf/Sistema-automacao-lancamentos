from unittest.mock import patch

from services.document_analyzer import DocumentAnalyzer


def _criar_analyzer() -> DocumentAnalyzer:
    with patch("services.document_analyzer.anythingllm_client.disponivel", return_value=True):
        return DocumentAnalyzer()


DADOS_FATURA = {
    "emitente_nome": "Sonangol Distribuidora Lda",
    "emitente_nif": "123456789",
    "adquirente_nome": "Cliente Teste Lda",
    "adquirente_nif": "987654321",
    "numero_fatura": "FT 2026/001",
    "valor_total_aoa": "150.000,00",
    "data_emissao": "15/03/2024",
    "tipo_documento": "fatura",
}


def test_prompt_contem_todos_os_dados_extraidos():
    analyzer = _criar_analyzer()
    prompt = analyzer._construir_prompt_classificacao("texto do documento", DADOS_FATURA)

    assert "Sonangol Distribuidora Lda" in prompt
    assert "123456789" in prompt
    assert "Cliente Teste Lda" in prompt
    assert "987654321" in prompt
    assert "FT 2026/001" in prompt
    assert "150.000,00" in prompt
    assert "15/03/2024" in prompt
    assert "fatura" in prompt
    assert "texto do documento" in prompt


def test_prompt_pede_json_com_todos_os_tipos_validos():
    analyzer = _criar_analyzer()
    prompt = analyzer._construir_prompt_classificacao("texto", DADOS_FATURA)

    for tipo in [
        "compra_mercadoria", "compra_servico", "venda_mercadoria",
        "prestacao_servico", "pagamento_fornecedor", "recebimento_cliente",
        "a_classificar",
    ]:
        assert tipo in prompt


def test_prompt_lida_com_dados_em_falta_sem_rebentar():
    analyzer = _criar_analyzer()
    prompt = analyzer._construir_prompt_classificacao("", {})

    assert "(não identificado)" in prompt
    assert "desconhecido" in prompt


def test_prompt_trunca_excerto_em_2000_caracteres():
    analyzer = _criar_analyzer()
    texto_longo = "A" * 5000
    prompt = analyzer._construir_prompt_classificacao(texto_longo, DADOS_FATURA)

    assert "A" * 2000 in prompt
    assert "A" * 2001 not in prompt


# Tamanho do prompt fixo (sem excerto, sem dados) ANTES da Fase 8 — medido
# a partir do texto literal usado em document_analyzer.py antes desta
# otimização, para a comparação abaixo ser uma medição real, não um limite
# arbitrário (ver secção 11 do mapa de impacto: MEDIR antes de otimizar).
_PROMPT_FIXO_ANTES_DA_FASE_8 = 945


def test_texto_fixo_do_prompt_encolheu_sem_perder_campos():
    """Fase 8 — o texto fixo (instruções/rótulos, sem contar o excerto
    variável nem os valores extraídos) deve ser mensuravelmente mais
    pequeno do que antes desta otimização, sem perder nenhum campo
    (cobertura de campos já garantida pelos testes acima)."""
    analyzer = _criar_analyzer()
    prompt_sem_excerto = analyzer._construir_prompt_classificacao("", {})

    assert len(prompt_sem_excerto) < _PROMPT_FIXO_ANTES_DA_FASE_8, (
        f"prompt fixo ({len(prompt_sem_excerto)} caracteres) já não é mais "
        f"pequeno do que antes da Fase 8 ({_PROMPT_FIXO_ANTES_DA_FASE_8})"
    )
    reducao_percentual = 100 * (1 - len(prompt_sem_excerto) / _PROMPT_FIXO_ANTES_DA_FASE_8)
    assert reducao_percentual >= 15, f"redução de só {reducao_percentual:.1f}% — pouco significativa"
