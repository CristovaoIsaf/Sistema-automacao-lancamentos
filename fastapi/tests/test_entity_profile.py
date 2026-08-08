from services import entity_profile
from services import pgc as pgc_ao


def test_tipo_dominante_sem_nif_devolve_none():
    assert entity_profile.tipo_dominante(None) is None


def test_tipo_dominante_sem_historico_devolve_none():
    assert entity_profile.tipo_dominante("nif-inexistente-001") is None


def test_registrar_ignora_sem_nif():
    entity_profile.registrar_classificacao(None, pgc_ao.TIPO_VENDA_MERCADORIA)
    assert entity_profile.tipo_dominante(None) is None


def test_registrar_ignora_tipo_a_classificar():
    nif = "nif-ignora-a-classificar"
    for _ in range(5):
        entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_A_CLASSIFICAR)
    assert entity_profile.tipo_dominante(nif) is None


def test_tipo_dominante_devolve_none_com_poucos_documentos():
    nif = "nif-poucos-documentos"
    for _ in range(entity_profile.MINIMO_DOCUMENTOS - 1):
        entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_VENDA_MERCADORIA)
    assert entity_profile.tipo_dominante(nif) is None


def test_tipo_dominante_devolve_tipo_apos_minimo_de_documentos_unanimes():
    nif = "nif-unanime"
    for i in range(entity_profile.MINIMO_DOCUMENTOS):
        entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_VENDA_MERCADORIA, f"fp-unanime-{i}")
    assert entity_profile.tipo_dominante(nif) == pgc_ao.TIPO_VENDA_MERCADORIA


def test_tipo_dominante_devolve_none_com_historico_misto():
    nif = "nif-misto"
    entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_VENDA_MERCADORIA, "fp-misto-1")
    entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_VENDA_MERCADORIA, "fp-misto-2")
    entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_COMPRA_MERCADORIA, "fp-misto-3")
    assert entity_profile.tipo_dominante(nif) is None


def test_registrar_e_idempotente_por_fingerprint():
    nif = "nif-idempotente"
    for _ in range(10):
        entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_VENDA_MERCADORIA, "fp-mesmo-documento")

    # 10 reanálises do MESMO documento contam como 1 só — não atinge o
    # mínimo de documentos exigido.
    assert entity_profile.tipo_dominante(nif) is None


def test_registrar_sem_fingerprint_conta_sempre():
    nif = "nif-sem-fingerprint"
    for _ in range(entity_profile.MINIMO_DOCUMENTOS):
        entity_profile.registrar_classificacao(nif, pgc_ao.TIPO_PRESTACAO_SERVICO)
    assert entity_profile.tipo_dominante(nif) == pgc_ao.TIPO_PRESTACAO_SERVICO
