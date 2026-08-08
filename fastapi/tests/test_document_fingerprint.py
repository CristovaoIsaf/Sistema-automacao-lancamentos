from services.document_fingerprint import calcular_fingerprint


def test_fingerprint_de_bytes_vazios_e_o_vetor_de_teste_sha256_padrao():
    # SHA-256("") — valor universalmente conhecido, serve de documentação
    # do algoritmo/formato usado (hex minúsculo).
    assert calcular_fingerprint(b"") == (
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    )


def test_fingerprint_e_deterministico():
    conteudo = b"fatura de teste"
    assert calcular_fingerprint(conteudo) == calcular_fingerprint(conteudo)


def test_fingerprint_muda_com_o_conteudo():
    assert calcular_fingerprint(b"documento A") != calcular_fingerprint(b"documento B")


def test_fingerprint_tem_64_caracteres_hexadecimais_minusculos():
    resultado = calcular_fingerprint(b"qualquer coisa")
    assert len(resultado) == 64
    assert resultado == resultado.lower()
    assert all(c in "0123456789abcdef" for c in resultado)
