import os
from datetime import datetime

from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.concurrency import run_in_threadpool

from fastapi.app.config.settings import settings
from fastapi.app.models.schemas import OCRResponse
from fastapi.app.services.ocr_service import OCRService

router = APIRouter()
ocr_service = OCRService()


@router.post("/ocr/extract", tags=["OCR"], response_model=OCRResponse)
async def extract_text(
    file: UploadFile = File(...),
    preprocess: bool = True,
    extract_accounting: bool = True,
):
    """
    Extrai texto de documentos via OCR

    - Suporta PDF, PNG, JPG, JPEG, TIFF, BMP
    - Extrai dados contábeis automaticamente
    - Retorna texto e metadados
    """
    file_ext = os.path.splitext(file.filename)[1].lower()
    if file_ext not in settings.ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Formato não suportado. Formatos permitidos: {settings.ALLOWED_EXTENSIONS}",
        )

    content = await file.read()
    if len(content) > settings.MAX_UPLOAD_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"Arquivo muito grande. Tamanho máximo: {settings.MAX_UPLOAD_SIZE / 1024 / 1024:.0f}MB",
        )

    # O Tesseract é bloqueante — corre numa threadpool para não travar o event loop.
    # Também deixa de precisar de gravar ficheiro temporário: extract_text_from_bytes
    # trabalha diretamente com os bytes recebidos.
    result = await run_in_threadpool(
        ocr_service.extract_text_from_bytes, content, file.filename, preprocess
    )

    if not result["success"]:
        raise HTTPException(status_code=500, detail=result.get("error", "Erro no OCR"))

    response_data = {
        "success": True,
        "text": result["text"],
        "confidence": result["confidence"],
        "metadata": result["metadata"],
        "filename": file.filename,
        "processed_at": datetime.now().isoformat(),
    }

    if extract_accounting and result["text"]:
        response_data["accounting_data"] = ocr_service.extract_accounting_data(result["text"])

    return response_data


@router.get("/health/ocr", tags=["Health"])
async def check_ocr():
    """Verifica se o OCR está funcionando."""
    try:
        from PIL import Image
        import pytesseract

        img = Image.new("RGB", (100, 30), color="white")
        pytesseract.image_to_string(img)

        return {
            "status": "ok",
            "tesseract_version": str(pytesseract.get_tesseract_version()),
            "message": "OCR funcionando corretamente",
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.get("/categories", tags=["Contabilidade"])
async def get_accounting_categories():
    """
    Retorna as categorias contábeis suportadas.
    Baseado no Plano de Contas Geral Angolano (PGC).
    """
    return {
        "success": True,
        "categories": settings.ACCOUNTING_CATEGORIES,
        "plan": "PGC - Plano de Contas Geral Angolano",
    }