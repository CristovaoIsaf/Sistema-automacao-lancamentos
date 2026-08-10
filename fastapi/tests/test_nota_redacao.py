from unittest.mock import patch

from services import nota_redacao
from services.nota_redacao import GrupoEntidadeRedacaoDTO, NotaRedacaoRequest


def _dados(**overrides) -> NotaRedacaoRequest:
    base = dict(
        conta="32",
        nomeConta="Fornecedores",
        natureza="CREDORA",
        inicio="2026-01-01",
        fim="2026-12-31",
        totalDebito="20000.00",
        totalCredito="80000.00",
        saldo="-60000.00",
        porEntidade=[
            GrupoEntidadeRedacaoDTO(entidade="Fornecedor XPTO", tipo="FORNECEDOR", totalDebito="20000.00", totalCredito="80000.00"),
        ],
    )
    base.update(overrides)
    return NotaRedacaoRequest(**base)


def test_redigir_template_inclui_conta_nome_periodo_e_totais():
    texto = nota_redacao.redigir_template(_dados())

    assert "32" in texto
    assert "Fornecedores" in texto
    assert "2026-01-01" in texto and "2026-12-31" in texto
    assert "20000.00 AOA" in texto
    assert "80000.00 AOA" in texto


def test_redigir_template_conta_credora_com_saldo_negativo_le_se_como_credor():
    texto = nota_redacao.redigir_template(_dados(natureza="CREDORA", saldo="-60000.00"))
    assert "saldo credor de 60000.00 AOA" in texto


def test_redigir_template_conta_devedora_com_saldo_positivo_le_se_como_devedor():
    texto = nota_redacao.redigir_template(_dados(natureza="DEVEDORA", totalDebito="80000.00", totalCredito="20000.00", saldo="60000.00"))
    assert "saldo devedor de 60000.00 AOA" in texto


def test_redigir_template_sem_movimentos_diz_explicitamente_que_nao_houve_movimento():
    texto = nota_redacao.redigir_template(_dados(porEntidade=[], totalDebito="0.00", totalCredito="0.00", saldo="0.00"))
    assert "Não foram registados movimentos" in texto


def test_redigir_template_incluiu_cada_entidade_do_grupo():
    dados = _dados(porEntidade=[
        GrupoEntidadeRedacaoDTO(entidade="Fornecedor A", totalDebito="1000.00", totalCredito="0.00"),
        GrupoEntidadeRedacaoDTO(entidade="Fornecedor B", totalDebito="0.00", totalCredito="500.00"),
    ])
    texto = nota_redacao.redigir_template(dados)
    assert "Fornecedor A" in texto
    assert "Fornecedor B" in texto
    assert "2 entidades" in texto


def test_redigir_sem_ia_disponivel_usa_template():
    with patch("services.nota_redacao.anythingllm_client.disponivel", return_value=False):
        resultado = nota_redacao.redigir(_dados())

    assert resultado["fonte"] == "template"
    assert resultado["texto"] == nota_redacao.redigir_template(_dados())


def test_redigir_com_ia_disponivel_e_resposta_valida_usa_texto_da_ia():
    dados = _dados()
    resposta_ia = "No exercício de 2026, a conta 32 — Fornecedores apresentou um saldo credor de 60000.00 AOA."

    with patch("services.nota_redacao.anythingllm_client.disponivel", return_value=True), \
         patch("services.nota_redacao.anythingllm_client.consultar_workspace", return_value={"resposta": resposta_ia, "fontes": []}):
        resultado = nota_redacao.redigir(dados)

    assert resultado["fonte"] == "ia"
    assert resultado["texto"] == resposta_ia


def test_redigir_com_ia_a_inventar_numero_novo_cai_para_template():
    # Fase 14 — "não pode inventar valores": 999999.99 não existe em
    # nenhum campo de `dados`, por isso a resposta da IA é descartada.
    dados = _dados()
    resposta_ia_com_numero_inventado = "A conta apresentou um saldo credor de 999999.99 AOA."

    with patch("services.nota_redacao.anythingllm_client.disponivel", return_value=True), \
         patch("services.nota_redacao.anythingllm_client.consultar_workspace", return_value={"resposta": resposta_ia_com_numero_inventado, "fontes": []}):
        resultado = nota_redacao.redigir(dados)

    assert resultado["fonte"] == "template"
    assert "999999.99" not in resultado["texto"]


def test_redigir_com_ia_indisponivel_por_excecao_cai_para_template():
    dados = _dados()
    with patch("services.nota_redacao.anythingllm_client.disponivel", return_value=True), \
         patch("services.nota_redacao.anythingllm_client.consultar_workspace", side_effect=nota_redacao.anythingllm_client.AnythingLLMError("falhou")):
        resultado = nota_redacao.redigir(dados)

    assert resultado["fonte"] == "template"


def test_redigir_com_ia_resposta_vazia_cai_para_template():
    dados = _dados()
    with patch("services.nota_redacao.anythingllm_client.disponivel", return_value=True), \
         patch("services.nota_redacao.anythingllm_client.consultar_workspace", return_value={"resposta": "", "fontes": []}):
        resultado = nota_redacao.redigir(dados)

    assert resultado["fonte"] == "template"
