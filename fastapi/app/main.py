import time
from typing import Optional

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware

from config.settings import settings
from services.document_fingerprint import calcular_fingerprint
from services.metricas import metricas
from services.ocr_service import OCRService
from services.document_analyzer import DocumentAnalyzer
from services import entity_profile
from services import nota_redacao
from services import pgc as pgc_ao

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


@app.get("/metricas")
def obter_metricas():
    """Fase 12 — contadores e tempos médios agregados desde o arranque
    deste processo (ver services/metricas.py). Só para inspeção/depuração
    (thesis), não é um endpoint de produção com autenticação."""
    return metricas.resumo()


@app.get("/pgc/contas")
@app.get("/api/v1/pgc/contas")
def pgc_contas():
    """Fase 6 do plano de 20 fases — plano de contas oficial (Decreto
    82/01, ver services/pgc.py), fonte única consumida pelo backend Java
    (PlanoContasClient) em vez de uma cópia hardcoded lá."""
    return pgc_ao.plano_de_contas()


@app.get("/perfil-entidade/{nif}")
@app.get("/api/v1/perfil-entidade/{nif}")
def perfil_entidade(nif: str):
    """Fase 12 do plano de 20 fases — "Perfil de Entidade": o
    conhecimento acumulado por entity_profile.py (ver
    document_analyzer.py._classificar_por_perfil_entidade) já existia,
    mas só era usado internamente para poupar chamadas de IA. Este
    endpoint expõe-o para o dossiê da entidade (ver EntidadeController
    no lado Java)."""
    return entity_profile.resumo(nif)


@app.post("/notas/redacao")
@app.post("/api/v1/notas/redacao")
def notas_redacao(dados: nota_redacao.NotaRedacaoRequest):
    """Fase 14 do plano de 20 fases — "Notas às Contas": recebe os dados
    JÁ CALCULADOS de uma nota (ver NotaContaService no lado Java) e
    devolve um rascunho de texto explicativo — nunca calcula nem inventa
    valores aqui, só formata/redige (ver services/nota_redacao.py)."""
    return nota_redacao.redigir(dados)


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
async def analisar(
    ficheiro: UploadFile = File(...),
    preprocess: bool = True,
    fingerprint: Optional[str] = Form(None),
    empresa_atividade_economica: Optional[str] = Form(None),
    empresa_natureza_negocio: Optional[str] = Form(None),
):
    """
    PIPELINE REAL (substitui os dados fixos anteriores):
      1. OCR do ficheiro recebido (imagem ou PDF).
      2. Extração de dados contábeis (regex).
      3. Classificação do tipo (Gemini com fallback a regras).
      4. Geração do lançamento por partidas dobradas com contas do Decreto 82/01.
    O backend Spring envia aqui os bytes do documento (multipart 'ficheiro').

    `fingerprint` (Fase 4 — ver services/document_fingerprint.py): o
    backend Java já calculou o SHA-256 do documento no upload
    (DocumentoContabilistico.hashConteudo) e envia-o aqui para não
    recalcular à toa nem arriscar duas implementações divergirem — é
    opcional só para permitir chamar este endpoint directamente (fora do
    fluxo Java), caso em que é calculado aqui como fallback.

    `empresa_atividade_economica`/`empresa_natureza_negocio` (Fase 5 do
    plano de 20 fases — classificação contabilística): contexto da
    empresa (ver ContextoClassificacaoService no lado Java), sempre
    disponível e enviado em toda análise — nunca inferido pela IA, só
    configurado por um Administrador em /configuracoes.
    """
    inicio_total = time.monotonic()

    conteudo = await ficheiro.read()
    if not conteudo:
        raise HTTPException(status_code=400, detail="Ficheiro vazio.")

    fingerprint_final = fingerprint or calcular_fingerprint(conteudo)

    contexto_empresa = None
    if empresa_atividade_economica or empresa_natureza_negocio:
        contexto_empresa = {
            "atividade_economica": empresa_atividade_economica,
            "natureza_negocio": empresa_natureza_negocio,
        }

    # Tesseract é bloqueante — corre numa threadpool. Fase 5: usa a
    # variante com cache, que evita repetir o OCR se este fingerprint já
    # tiver sido processado antes (ex: reanálise do mesmo documento).
    inicio_ocr = time.monotonic()
    ocr_res = await run_in_threadpool(
        ocr_service.extract_text_from_bytes_cacheado,
        conteudo, ficheiro.filename, preprocess, fingerprint_final,
    )
    metricas.registar_duracao("ocr", time.monotonic() - inicio_ocr)
    metricas.incrementar("ocr_cache_hit" if ocr_res.get("cache_hit") else "ocr_cache_miss")

    if not ocr_res.get("success"):
        metricas.incrementar("analises_falhadas_ocr")
        raise HTTPException(status_code=400, detail=ocr_res.get("error", "Erro no OCR"))

    texto = ocr_res.get("text", "") or ""
    # extract_invoice_data (não a extract_accounting_data antiga, que só
    # devolvia 4 campos "por compatibilidade") — dá ao analyzer os dados já
    # tratados por regex (emitente, adquirente, nº fatura, valor, IVA...),
    # para a IA não ter de os re-extrair do texto em bruto. Fase 6: passa o
    # fingerprint para reaproveitar a validação já calculada, se aplicável.
    extraido = ocr_service.extract_invoice_data(texto, fingerprint_final)

    # Fase 9: passa o fingerprint para reaproveitar a classificação de IA
    # já calculada, se este documento ambíguo já tiver sido analisado.
    # Fase 11 — concorrência: analyze_document pode chamar a IA
    # (httpx síncrono, ver anythingllm_client.py) e demorar dezenas de
    # segundos. Sem run_in_threadpool, essa chamada bloquearia o event
    # loop do Uvicorn inteiro — TODOS os outros pedidos (incluindo
    # documentos completamente independentes, resolvíveis só por regras)
    # ficariam parados atrás desta única análise lenta.
    inicio_classificacao = time.monotonic()
    analise = await run_in_threadpool(
        analyzer.analyze_document,
        texto,
        {"dados_fatura": extraido["dados"], "confidence": ocr_res.get("confidence", 0)},
        fingerprint_final,
        contexto_empresa,
    )
    metricas.registar_duracao("classificacao", time.monotonic() - inicio_classificacao)

    # Fase 12 — de onde veio a classificação (ver document_analyzer.py):
    # "regras" (gate da Fase 7), "perfil_entidade" (Fase 10), ou
    # "anythingllm:..." (Fase 9 — cache ou chamada real, ver iaCacheHit).
    # Mede o que cada fase anterior está realmente a poupar.
    modelo = analise.get("modelo") or ""
    if modelo == "regras":
        metricas.incrementar("gate_regras")
    elif modelo == "perfil_entidade":
        metricas.incrementar("gate_perfil_entidade")
    elif modelo.startswith("anythingllm"):
        metricas.incrementar("gate_ia_cache_hit" if analise.get("iaCacheHit") else "gate_ia_chamada_real")

    # Anexa contexto do OCR e o resultado da validação determinística
    # (Fase 3 — ver services/document_validation.py) para revisão humana
    # no frontend.
    analise["textoOcr"] = texto[:5000]
    analise["confiancaOcr"] = ocr_res.get("confidence", 0)
    analise["validacao"] = extraido["validacao"]
    analise["fingerprint"] = fingerprint_final
    analise["ocrCacheHit"] = ocr_res.get("cache_hit", False)

    validacao = extraido["validacao"] or {}
    metricas.incrementar("validacao_valida" if validacao.get("valido") else "validacao_invalida")
    metricas.incrementar("analises_total")
    metricas.registar_duracao("analisar_total", time.monotonic() - inicio_total)
    return analise