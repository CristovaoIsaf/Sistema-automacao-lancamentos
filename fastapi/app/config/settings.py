import os
import platform
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import ClassVar, Dict, List

from pydantic_settings import BaseSettings, SettingsConfigDict


def _localizar_tesseract() -> str:
    """
    Procura o executável do Tesseract no sistema, de forma multiplataforma.
    Chamada uma única vez, no import do módulo — não dentro da classe.
    """
    if platform.system() == "Windows":
        candidatos = [
            r"C:\Program Files\Tesseract-OCR\tesseract.exe",
            r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
            os.path.expanduser(r"~\AppData\Local\Tesseract-OCR\tesseract.exe"),
        ]
        for caminho in candidatos:
            if os.path.exists(caminho):
                return caminho

        encontrado = shutil.which("tesseract")
        if encontrado:
            return encontrado

        return r"C:\Program Files\Tesseract-OCR\tesseract.exe"  # fallback, mesmo sem confirmar

    # Linux / macOS
    encontrado = shutil.which("tesseract")
    return encontrado or "tesseract"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # App
    APP_TITLE: str = "AI Service - Sistema Contábil Angolano"
    APP_VERSION: str = "1.0.0"
    APP_DESCRIPTION: str = "Serviço de OCR e IA para automação contábil de PME angolanas"
    DEBUG: bool = True

    # CORS — origens de BROWSER (páginas), nunca chamadas servidor-a-servidor
    # (o Spring Boot chama este serviço diretamente, isso não passa pelo CORS
    # do browser, por isso a porta do backend não pertence aqui). Sobreponível
    # via variável de ambiente ALLOWED_ORIGINS (lista JSON) se um dia for
    # preciso, mas hoje nenhum ambiente a define.
    ALLOWED_ORIGINS: List[str] = [
        "http://localhost:5173",  # React+Vite dev
        "https://sistema-automacao-lancamentos.vercel.app",  # frontend em produção (Vercel)
    ]

    # Tesseract OCR
    TESSERACT_CMD: str = _localizar_tesseract()
    TESSERACT_LANG: str = "por+eng"  # Português + Inglês

    # Upload
    MAX_UPLOAD_SIZE: int = 10 * 1024 * 1024  # 10MB
    ALLOWED_EXTENSIONS: List[str] = [".pdf", ".png", ".jpg", ".jpeg", ".tiff", ".bmp"]

    # Diretórios (multiplataforma via tempfile/pathlib)
    TEMP_DIR: str = str(Path(tempfile.gettempdir()) / "ai_service")
    UPLOAD_DIR: str = str(Path(__file__).resolve().parent.parent / "uploads")

    # AnythingLLM (RAG) — dois workspaces separados:
    #   NORMAS   = texto do Decreto 82/01, usado só para fundamentar/citar a
    #              base legal da classificação (campo "fundamentacao").
    #   EXEMPLOS = documentos de exemplo já classificados, usado para orientar
    #              a própria classificação (tipo/valor/entidade) por semelhança.
    ANYTHINGLLM_BASE_URL: str = os.getenv("ANYTHINGLLM_BASE_URL", "http://localhost:3001")
    ANYTHINGLLM_API_KEY: str = os.getenv("ANYTHINGLLM_API_KEY", "")
    ANYTHINGLLM_WORKSPACE_NORMAS: str = os.getenv("ANYTHINGLLM_WORKSPACE_NORMAS", "workspace-normativo")
    ANYTHINGLLM_WORKSPACE_EXEMPLOS: str = os.getenv("ANYTHINGLLM_WORKSPACE_EXEMPLOS", "regras-do-negocio")

    # Google Gemini (usado diretamente pelo DocumentAnalyzer para sugerir lançamentos)
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")

    # Categorias contábeis angolanas
    ACCOUNTING_CATEGORIES: ClassVar[Dict[str, List[str]]] = {
        "ativos": ["caixa", "bancos", "clientes", "estoques", "equipamentos"],
        "passivos": ["fornecedores", "empréstimos", "impostos_pagar"],
        "receitas": ["vendas", "serviços", "juros_recebidos"],
        "despesas": ["salários", "aluguel", "energia", "água", "transporte"],
        "impostos": ["iva", "irt", "selo"],
    }


settings = Settings()

# Criar diretórios necessários (não falha se já existirem)
os.makedirs(settings.TEMP_DIR, exist_ok=True)
os.makedirs(settings.UPLOAD_DIR, exist_ok=True)