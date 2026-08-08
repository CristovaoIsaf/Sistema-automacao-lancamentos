from services.metricas import Metricas


def test_incrementar_soma_por_omissao_um():
    m = Metricas()
    m.incrementar("evento_a")
    m.incrementar("evento_a")
    assert m.resumo()["contadores"]["evento_a"] == 2


def test_incrementar_aceita_quantidade_explicita():
    m = Metricas()
    m.incrementar("evento_b", 5)
    assert m.resumo()["contadores"]["evento_b"] == 5


def test_contadores_diferentes_sao_independentes():
    m = Metricas()
    m.incrementar("a")
    m.incrementar("b")
    m.incrementar("b")
    contadores = m.resumo()["contadores"]
    assert contadores["a"] == 1
    assert contadores["b"] == 2


def test_registar_duracao_calcula_media():
    m = Metricas()
    m.registar_duracao("fase_x", 1.0)
    m.registar_duracao("fase_x", 3.0)
    resumo = m.resumo()
    assert resumo["tempos_medios_segundos"]["fase_x"] == 2.0
    assert resumo["tempos_contagem"]["fase_x"] == 2


def test_resumo_sem_dados_devolve_estruturas_vazias():
    m = Metricas()
    resumo = m.resumo()
    assert resumo["contadores"] == {}
    assert resumo["tempos_medios_segundos"] == {}
    assert resumo["tempos_contagem"] == {}


def test_instancias_diferentes_nao_partilham_estado():
    m1 = Metricas()
    m2 = Metricas()
    m1.incrementar("evento")
    assert m2.resumo()["contadores"] == {}
