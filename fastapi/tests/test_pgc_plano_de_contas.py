from services import pgc as pgc_ao


def test_plano_de_contas_nao_tem_codigos_duplicados():
    codigos = [c["codigo"] for c in pgc_ao.plano_de_contas()]
    assert len(codigos) == len(set(codigos))


def test_plano_de_contas_inclui_contas_conhecidas_com_classe_e_natureza():
    por_codigo = {c["codigo"]: c for c in pgc_ao.plano_de_contas()}

    clientes = por_codigo["31"]
    assert clientes["nome"] == "Clientes"
    assert clientes["classe"] == "3"
    assert clientes["natureza"] == "DEVEDORA"
    assert clientes["subconta"] is None

    fornecedores = por_codigo["32"]
    assert fornecedores["natureza"] == "CREDORA"


def test_plano_de_contas_subconta_so_marcada_quando_codigo_tem_ponto():
    por_codigo = {c["codigo"]: c for c in pgc_ao.plano_de_contas()}

    agua = por_codigo["75.2.11"]
    assert agua["subconta"] == "75.2.11"
    assert agua["classe"] == "7"

    fse = por_codigo["75"]
    assert fse["subconta"] is None


def test_plano_de_contas_iva_dedutivel_e_liquidado_tem_naturezas_opostas():
    por_codigo = {c["codigo"]: c for c in pgc_ao.plano_de_contas()}

    assert por_codigo["34.5.1"]["natureza"] == "DEVEDORA"
    assert por_codigo["34.5.2"]["natureza"] == "CREDORA"
