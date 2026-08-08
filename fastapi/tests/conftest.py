import sys
from pathlib import Path

# fastapi/app/ é a raiz de import em produção (ver Dockerfile: COPY app/ .,
# uvicorn main:app corre com /app == fastapi/app). Os testes correm a
# partir de fastapi/, por isso precisam do mesmo sys.path para "from
# services.x import y" resolver igual em teste e em produção.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "app"))
