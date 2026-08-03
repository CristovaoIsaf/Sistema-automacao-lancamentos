from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware

from config.settings import settings
from services.ocr_service import OCRService
from services.document_analyzer import DocumentAnalyzer

app = FastAPI(
    title="AI Service",
    version="1.0.0",
    description="Serviço de OCR e Inteligência Artificial (PGC-AO)",
)

# Restrito às origens em settings.ALLOWED_ORIGINS (config/settings.py) — em vez
# de "*", que combinado com allow_credentials=True nem é válido pela spec CORS
# (o browser rejeita wildcard quando há credenciais). Em produção, define a
# variável de ambiente ALLOWED_ORIGINS com o domínio real do frontend.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ocr_service = OCRService()
analyzer = DocumentAnalyzer()


@app.get("/")
def root():
    return {"service": "AI Service", "status": "online"}


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/ocr")
async def ocr(ficheiro: UploadFile = File(...), preprocess: bool = True):
    """Recebe uma imagem ou PDF e devolve o texto extraído (só OCR)."""
    conteudo = await ficheiro.read()
    resultado = await run_in_threadpool(
        ocr_service.extract_text_from_bytes, conteudo, ficheiro.filename, preprocess
    )
    if not resultado["success"]:
        raise HTTPException(status_code=400, detail=resultado.get("error", "Erro no OCR"))

    dados_contabeis = ocr_service.extract_accounting_data(resultado["text"])
    return {
        "ficheiro": ficheiro.filename,
        "texto": resultado["text"],
        "confianca": resultado["confidence"],
        "palavras": resultado["word_count"],
        "dados_contabeis": dados_contabeis,
    }


@app.post("/analisar")
@app.post("/api/v1/analisar")
async def analisar(ficheiro: UploadFile = File(...), preprocess: bool = True):
    """
    PIPELINE REAL (substitui os dados fixos anteriores):
      1. OCR do ficheiro recebido (imagem ou PDF).
      2. Extração de dados contábeis (regex).
      3. Classificação do tipo (Gemini com fallback a regras).
      4. Geração do lançamento por partidas dobradas com contas do Decreto 82/01.
    O backend Spring envia aqui os bytes do documento (multipart 'ficheiro').
    """
    conteudo = await ficheiro.read()
    if not conteudo:
        raise HTTPException(status_code=400, detail="Ficheiro vazio.")

    # Tesseract é bloqueante — corre numa threadpool.
    ocr_res = await run_in_threadpool(
        ocr_service.extract_text_from_bytes, conteudo, ficheiro.filename, preprocess
    )
    if not ocr_res.get("success"):
        raise HTTPException(status_code=400, detail=ocr_res.get("error", "Erro no OCR"))

    texto = ocr_res.get("text", "") or ""
    accounting = ocr_service.extract_accounting_data(texto)

    analise = analyzer.analyze_document(
        texto,
        {"accounting_data": accounting, "confidence": ocr_res.get("confidence", 0)},
    )

    # Anexa contexto do OCR para revisão humana no frontend.
    analise["textoOcr"] = texto[:5000]
    analise["confiancaOcr"] = ocr_res.get("confidence", 0)
    return analise