import io
import logging
from dataclasses import asdict
from typing import Any, Dict, List, Optional

import cv2
import numpy as np
import pytesseract
from PIL import Image
from pdf2image import convert_from_bytes

from config.settings import settings
from services.cache_versionado import CacheVersionado
from services.document_validation import validar_documento_com_cache
from services.regex_extract import (
    extrair_dados_fatura,
    gerar_relatorio_qualidade,
    detectar_tipo_documento,
)

logger = logging.getLogger(__name__)

# Fase 5 do mapa de impacto — evita repetir OCR (Tesseract, o passo mais
# lento do pipeline) para um documento já processado antes com o mesmo
# fingerprint. A versão de cache inclui idioma/pré-processamento porque
# alteram o resultado do OCR para os MESMOS bytes.
OCR_CACHE_VERSION = "TFC-2026-v1"
_cache_ocr: CacheVersionado = CacheVersionado("ocr")


class OCRService:
    """Serviço de OCR otimizado para documentos contábeis angolanos."""

    def __init__(self):
        pytesseract.pytesseract.tesseract_cmd = settings.TESSERACT_CMD
        self.lang = settings.TESSERACT_LANG

        try:
            version = pytesseract.get_tesseract_version()
            logger.info(f"Tesseract {version} configurado com sucesso")
        except Exception as e:
            logger.error(f"Erro ao configurar Tesseract: {e}")
            logger.error(f"Caminho configurado: {settings.TESSERACT_CMD}")

    # ── Pré-processamento ─────────────────────────────────────────────────

    def preprocess_image(self, image: np.ndarray) -> np.ndarray:
        """Pré-processamento de imagem para melhor OCR."""
        try:
            if len(image.shape) == 3:
                gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            else:
                gray = image

            denoised = cv2.fastNlMeansDenoising(gray, None, 10, 7, 21)

            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
            enhanced = clahe.apply(denoised)

            binary = cv2.adaptiveThreshold(
                enhanced, 255,
                cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                cv2.THRESH_BINARY, 31, 2,
            )
            return binary
        except Exception as e:
            logger.warning(f"Erro no pré-processamento, usando imagem original: {e}")
            return image

    # ── Extração de texto ────────────────────────────────────────────────

    def _extrair_de_array(self, image: np.ndarray, preprocess: bool) -> Dict[str, Any]:
        """Roda o Tesseract sobre uma única imagem já carregada em memória."""
        processed_image = self.preprocess_image(image) if preprocess else image

        text = pytesseract.image_to_string(
            processed_image, lang=self.lang, config="--oem 3 --psm 6"
        )

        data = pytesseract.image_to_data(
            processed_image, lang=self.lang, output_type=pytesseract.Output.DICT
        )
        confidence_scores = [
            int(conf) for conf in data["conf"] if conf != "-1" and int(conf) > 0
        ]
        avg_confidence = float(np.mean(confidence_scores)) if confidence_scores else 0.0

        return {
            "text": text.strip(),
            "confidence": avg_confidence,
            "word_count": len(text.split()),
            "metadata": {"image_size": image.shape},
        }

    def extract_text_from_bytes(
        self, conteudo: bytes, filename: str, preprocess: bool = True
    ) -> Dict[str, Any]:
        """
        Extrai texto a partir dos bytes de um ficheiro (imagem ou PDF),
        sem gravar em disco.
        """
        try:
            extensao = filename.lower().rsplit(".", 1)[-1]
            logger.info(f"Processando: {filename} ({len(conteudo) / 1024:.2f} KB)")

            if extensao == "pdf":
                resultado = self._extract_from_pdf_bytes(conteudo, preprocess)
            elif extensao in ("jpg", "jpeg", "png", "bmp", "tiff"):
                resultado = self._extract_from_image_bytes(conteudo, preprocess)
            else:
                raise ValueError(f"Formato não suportado: .{extensao}")

            logger.info(
                f"Texto extraído: {resultado['word_count']} palavras, "
                f"confiança média {resultado['confidence']:.2f}%"
            )
            resultado["metadata"]["filename"] = filename

            return {"success": True, "language": self.lang, **resultado}

        except Exception as e:
            logger.error(f"Erro no OCR de {filename}: {e}")
            return {"success": False, "error": str(e)}

    def extract_text_from_bytes_cacheado(
        self, conteudo: bytes, filename: str, preprocess: bool, fingerprint: Optional[str]
    ) -> Dict[str, Any]:
        """Fase 5: variante de extract_text_from_bytes que evita repetir o
        OCR para o mesmo fingerprint (ex: reanálise do mesmo documento).
        Sem fingerprint, corre sempre o OCR de novo — não há chave de
        cache válida. Devolve o mesmo formato de extract_text_from_bytes
        com uma chave adicional "cache_hit"."""
        if not fingerprint:
            resultado = self.extract_text_from_bytes(conteudo, filename, preprocess)
            return {**resultado, "cache_hit": False}

        versao = f"{OCR_CACHE_VERSION}:{self.lang}:{preprocess}"
        resultado, veio_do_cache = _cache_ocr.obter_ou_calcular(
            fingerprint, versao,
            lambda: self.extract_text_from_bytes(conteudo, filename, preprocess),
        )
        return {**resultado, "cache_hit": veio_do_cache}

    def _extract_from_image_bytes(self, conteudo: bytes, preprocess: bool) -> Dict[str, Any]:
        pil_image = Image.open(io.BytesIO(conteudo)).convert("RGB")
        image = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        return self._extrair_de_array(image, preprocess)

    def _extract_from_pdf_bytes(self, conteudo: bytes, preprocess: bool) -> Dict[str, Any]:
        paginas = convert_from_bytes(conteudo)
        resultados_por_pagina: List[Dict[str, Any]] = []

        for pagina in paginas:
            image = cv2.cvtColor(np.array(pagina.convert("RGB")), cv2.COLOR_RGB2BGR)
            resultados_por_pagina.append(self._extrair_de_array(image, preprocess))

        texto_completo = "\n\n".join(r["text"] for r in resultados_por_pagina)
        confiancas = [r["confidence"] for r in resultados_por_pagina if r["confidence"] > 0]

        return {
            "text": texto_completo.strip(),
            "confidence": float(np.mean(confiancas)) if confiancas else 0.0,
            "word_count": len(texto_completo.split()),
            "metadata": {"paginas": len(paginas)},
        }

    # ── Extração de dados contábeis ─────────────────────────────────────

    def extract_accounting_data(self, text: str) -> Dict[str, Any]:
        """
        Extrai informações contábeis do texto (compatível com o formato
        anterior — mantém as mesmas chaves para não quebrar quem já chama
        este método). Internamente agora usa a extração estruturada
        completa via `extract_invoice_data`.
        """
        if not text:
            return {
                "valores_monetarios": [],
                "datas": [],
                "nif": None,
                "tipo_documento": None,
            }

        dados = extrair_dados_fatura(text)

        # Mantém compatibilidade com quem consome estas 4 chaves específicas
        return {
            "valores_monetarios": [dados.valor_total_aoa] if dados.valor_total_aoa else [],
            "datas": [dados.data_emissao] if dados.data_emissao else [],
            "nif": dados.emitente_nif,
            "tipo_documento": dados.tipo_documento,
        }

    def extract_invoice_data(self, text: str, fingerprint: Optional[str] = None) -> Dict[str, Any]:
        """
        Extração completa e estruturada de fatura fiscal (padrão AGT):
        emitente, adquirente, NIFs (por proximidade ao rótulo, com fallback
        por ordem de aparição), número do documento, data/hora, valores,
        taxa de IVA, código hash e software certificado.

        Use este método quando precisar de todos os campos — o
        `extract_accounting_data` acima existe só por compatibilidade.

        `fingerprint` (Fase 6, opcional): quando fornecido, o resultado da
        validação determinística é reaproveitado do cache se este mesmo
        documento já tiver sido validado antes (ver
        document_validation.validar_documento_com_cache).
        """
        if not text:
            vazio = _dados_vazios()
            return {
                "dados": asdict(vazio),
                "qualidade": {"campos_faltantes": [], "confianca_geral": "baixa"},
                "validacao": validar_documento_com_cache(vazio, fingerprint).to_dict(),
            }

        dados = extrair_dados_fatura(text)
        qualidade = gerar_relatorio_qualidade(dados)
        validacao = validar_documento_com_cache(dados, fingerprint)

        return {"dados": asdict(dados), "qualidade": qualidade, "validacao": validacao.to_dict()}

    def process_document(
        self, conteudo: bytes, filename: str, preprocess: bool = True
    ) -> Dict[str, Any]:
        """
        Pipeline completo: OCR do ficheiro + extração estruturada da fatura.
        Combina `extract_text_from_bytes` e `extract_invoice_data` num único
        resultado, incluindo a confiança do OCR (útil para decidir se vale
        a pena revisar manualmente o documento).
        """
        resultado_ocr = self.extract_text_from_bytes(conteudo, filename, preprocess)

        if not resultado_ocr.get("success"):
            return resultado_ocr

        extraido = self.extract_invoice_data(resultado_ocr["text"])

        return {
            "success": True,
            "ocr_confidence": resultado_ocr["confidence"],
            "word_count": resultado_ocr["word_count"],
            **extraido,
        }


def _dados_vazios():
    from services.regex_extract import DadosFatura
    return DadosFatura()