import threading
import time

from services.cache_versionado import CacheVersionado


def test_cache_miss_chama_calcular_e_guarda():
    cache = CacheVersionado("teste")
    chamadas = []

    def calcular():
        chamadas.append(1)
        return "resultado"

    valor, veio_do_cache = cache.obter_ou_calcular("fp1", "v1", calcular)

    assert valor == "resultado"
    assert veio_do_cache is False
    assert len(chamadas) == 1


def test_cache_hit_nao_chama_calcular_de_novo():
    cache = CacheVersionado("teste")
    chamadas = []

    def calcular():
        chamadas.append(1)
        return "resultado"

    cache.obter_ou_calcular("fp1", "v1", calcular)
    valor, veio_do_cache = cache.obter_ou_calcular("fp1", "v1", calcular)

    assert valor == "resultado"
    assert veio_do_cache is True
    assert len(chamadas) == 1  # não recalculou


def test_versao_diferente_e_tratada_como_cache_miss():
    cache = CacheVersionado("teste")
    cache.guardar("fp1", "v1", "antigo")

    valor, veio_do_cache = cache.obter_ou_calcular("fp1", "v2", lambda: "novo")

    assert valor == "novo"
    assert veio_do_cache is False


def test_fingerprints_diferentes_sao_independentes():
    cache = CacheVersionado("teste")
    cache.guardar("fp1", "v1", "A")
    cache.guardar("fp2", "v1", "B")

    assert cache.obter("fp1", "v1") == "A"
    assert cache.obter("fp2", "v1") == "B"


def test_obter_sem_entrada_devolve_none():
    cache = CacheVersionado("teste")
    assert cache.obter("inexistente", "v1") is None


# ── Concorrência (Fase 11) ──────────────────────────────────────────────

def test_pedidos_concorrentes_para_a_mesma_chave_so_calculam_uma_vez():
    """Cache stampede (secção 13 do mapa de impacto): duas threads a
    pedir a MESMA chave ao mesmo tempo nunca devem chamar calcular() duas
    vezes — a segunda espera pela primeira e reaproveita o resultado."""
    cache = CacheVersionado("teste-stampede")
    chamadas = []
    lock_contagem = threading.Lock()
    pode_terminar = threading.Event()
    entrou_em_calcular = threading.Event()

    def calcular_lento():
        with lock_contagem:
            chamadas.append(1)
        entrou_em_calcular.set()
        pode_terminar.wait(timeout=2)
        return "resultado"

    resultados = []

    def worker():
        resultados.append(cache.obter_ou_calcular("fp-stampede", "v1", calcular_lento))

    t1 = threading.Thread(target=worker)
    t2 = threading.Thread(target=worker)

    t1.start()
    assert entrou_em_calcular.wait(timeout=2), "t1 não chegou a calcular() a tempo"
    t2.start()
    time.sleep(0.1)  # dá tempo a t2 bloquear no lock da chave
    pode_terminar.set()

    t1.join(timeout=3)
    t2.join(timeout=3)

    assert len(chamadas) == 1, "calcular() foi chamado mais do que uma vez para a mesma chave"
    assert resultados[0][0] == "resultado"
    assert resultados[1][0] == "resultado"


def test_pedidos_para_chaves_diferentes_nao_se_bloqueiam_mutuamente():
    """Documentos independentes (secção 12): uma chave bloqueada em
    calcular() nunca deve atrasar outra chave completamente diferente."""
    cache = CacheVersionado("teste-paralelismo")
    eventos = []
    pode_terminar_a = threading.Event()
    entrou_em_a = threading.Event()

    def calcular_a():
        entrou_em_a.set()
        pode_terminar_a.wait(timeout=2)
        eventos.append("a-terminou")
        return "resultado-a"

    def calcular_b():
        eventos.append("b-calculou")
        return "resultado-b"

    t1 = threading.Thread(target=lambda: cache.obter_ou_calcular("fp-a", "v1", calcular_a))
    t1.start()
    assert entrou_em_a.wait(timeout=2), "t1 não chegou a calcular_a() a tempo"

    # 'b' tem de conseguir completar mesmo com 'a' ainda bloqueado.
    valor_b, veio_do_cache_b = cache.obter_ou_calcular("fp-b", "v1", calcular_b)

    assert valor_b == "resultado-b"
    assert veio_do_cache_b is False
    assert "b-calculou" in eventos
    assert "a-terminou" not in eventos, "b não devia ter tido de esperar por a"

    pode_terminar_a.set()
    t1.join(timeout=3)
