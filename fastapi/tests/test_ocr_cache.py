from unittest.mock import patch

from services.ocr_service import OCRService


def _resultado_ocr_falso():
    return {
        "success": True, "language": "por", "text": "texto extraido",
        "confidence": 90.0, "word_count": 2, "metadata": {},
    }


def test_segunda_chamada_com_mesmo_fingerprint_nao_repete_ocr():
    servico = OCRService()
    with patch.object(
        OCRService, "extract_text_from_bytes", return_value=_resultado_ocr_falso()
    ) as ocr_mock:
        r1 = servico.extract_text_from_bytes_cacheado(b"bytes", "f.png", True, "fp-igual")
        r2 = servico.extract_text_from_bytes_cacheado(b"bytes", "f.png", True, "fp-igual")

    assert ocr_mock.call_count == 1
    assert r1["cache_hit"] is False
    assert r2["cache_hit"] is True
    assert r2["text"] == "texto extraido"


def test_fingerprints_diferentes_repetem_ocr():
    servico = OCRService()
    with patch.object(
        OCRService, "extract_text_from_bytes", return_value=_resultado_ocr_falso()
    ) as ocr_mock:
        servico.extract_text_from_bytes_cacheado(b"bytesA", "a.png", True, "fp-a")
        servico.extract_text_from_bytes_cacheado(b"bytesB", "b.png", True, "fp-b")

    assert ocr_mock.call_count == 2


def test_sem_fingerprint_repete_ocr_sempre():
    servico = OCRService()
    with patch.object(
        OCRService, "extract_text_from_bytes", return_value=_resultado_ocr_falso()
    ) as ocr_mock:
        servico.extract_text_from_bytes_cacheado(b"bytes", "f.png", True, None)
        servico.extract_text_from_bytes_cacheado(b"bytes", "f.png", True, None)

    assert ocr_mock.call_count == 2


def test_preprocess_diferente_e_tratado_como_documento_diferente():
    servico = OCRService()
    with patch.object(
        OCRService, "extract_text_from_bytes", return_value=_resultado_ocr_falso()
    ) as ocr_mock:
        servico.extract_text_from_bytes_cacheado(b"bytes", "f.png", True, "fp-x")
        servico.extract_text_from_bytes_cacheado(b"bytes", "f.png", False, "fp-x")

    assert ocr_mock.call_count == 2
