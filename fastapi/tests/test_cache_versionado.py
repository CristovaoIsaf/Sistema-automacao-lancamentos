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
