"""
Métricas de observabilidade (Fase 12 do mapa de impacto) — contadores e
tempos médios em memória, para tornar MENSURÁVEL o impacto real das
otimizações das Fases 4-11 (taxa de cache hit no OCR/IA, quantas vezes a
IA foi realmente evitada pelo gate da Fase 7 ou pelo perfil da Fase 10,
latência por fase do pipeline). Exposto em GET /metricas.

Fecha o ciclo "MEDIR → IDENTIFICAR GARGALO → OTIMIZAR → MEDIR NOVAMENTE"
que a Fase 8 já tinha começado (log do tamanho do prompt) mas sem um
sítio central para consultar os números agregados.

Em memória de processo — mesmo racional das Fases 5/6/9/10: suficiente
para o âmbito actual, sem dependência nova (Prometheus/Grafana seriam
sobre-engenharia para este TFC — ver secção 14 do mapa de impacto).
Reinicia a zero sempre que o processo reinicia, tal como os caches.

Classe (não funções de módulo) pelo mesmo motivo de CacheVersionado: os
testes podem instanciar a sua própria Metricas() isolada, em vez de
partilhar o estado global usado em produção (ver `metricas` no fim do
ficheiro, a instância única realmente usada pelo main.py).
"""

from collections import Counter
from threading import Lock
from typing import Dict


class Metricas:
    def __init__(self) -> None:
        self._lock = Lock()
        self._contadores: Counter = Counter()
        self._tempos_soma: Counter = Counter()
        self._tempos_contagem: Counter = Counter()

    def incrementar(self, nome: str, quantidade: int = 1) -> None:
        with self._lock:
            self._contadores[nome] += quantidade

    def registar_duracao(self, nome: str, segundos: float) -> None:
        with self._lock:
            self._tempos_soma[nome] += segundos
            self._tempos_contagem[nome] += 1

    def resumo(self) -> Dict[str, object]:
        with self._lock:
            tempos_medios = {
                nome: round(self._tempos_soma[nome] / self._tempos_contagem[nome], 3)
                for nome in self._tempos_contagem
                if self._tempos_contagem[nome] > 0
            }
            return {
                "contadores": dict(self._contadores),
                "tempos_medios_segundos": tempos_medios,
                "tempos_contagem": dict(self._tempos_contagem),
            }


# Instância única usada pelo pipeline real (ver main.py). Testes devem
# criar as suas próprias instâncias Metricas() em vez de usar esta.
metricas = Metricas()
