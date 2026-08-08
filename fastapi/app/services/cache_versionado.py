"""
Cache em memória, versionado, por fingerprint — mecanismo genérico
reutilizado pelas Fases 5 (cache de OCR, ver ocr_service.py), 6 (cache de
validação, ver document_validation.py) e 9 (cache de IA, ver
document_analyzer.py). Uma só implementação em vez de três quase iguais
(ver secção 6 do mapa de impacto: não duplicar regras/mecanismos entre
ficheiros).

Cada entrada é indexada por (fingerprint, versão) — não só pelo
fingerprint. Isto significa que, se a versão da lógica que produziu o
valor mudar (ex: nova versão do motor de validação, ou do idioma/
pré-processamento do OCR), uma entrada antiga nunca é devolvida como se
fosse actual: é tratada como cache miss e recalculada, tal como pedido no
mapa de impacto ("Quando uma dessas versões tornar o resultado
incompatível, não reutilizar cegamente o cache antigo").

Fase 11 do mapa de impacto — concorrência segura: `obter_ou_calcular` usa
um lock POR CHAVE (fingerprint, versão), não um lock global. Isto
resolve dois requisitos do mapa de impacto ao mesmo tempo:
  - "Prevenir cache stampede" (secção 13): pedidos concorrentes para o
    MESMO fingerprint nunca chamam `calcular()` em paralelo — o segundo
    pedido espera pelo primeiro e reaproveita o resultado, em vez de
    repetir OCR/validação/IA à toa.
  - "Não bloquear documentos independentes" (secção 12): pedidos para
    fingerprints DIFERENTES nunca esperam uns pelos outros — só o lock
    da própria chave é usado, nunca um lock global durante `calcular()`.

Em memória de processo (sem Redis/BD) — suficiente para o âmbito actual
do projecto (um único processo FastAPI) e não introduz nenhuma
dependência nova (ver secção 14: não introduzir bibliotecas/serviços sem
verificar primeiro se já existe solução equivalente — não existe). Se um
dia for preciso partilhar entre vários processos/réplicas, troca-se só a
implementação interna, sem alterar esta interface.

Nota de âmbito: tal como `_armazenamento`, os locks por chave em
`_locks_por_chave` nunca são removidos (crescem com o nº de fingerprints
distintos vistos). Aceitável para o âmbito actual (mesmo racional já
aplicado ao próprio armazenamento nas Fases 5/6/9) — revisitar só se um
dia isto correr indefinidamente em produção com volume real.
"""

from threading import Lock
from typing import Callable, Dict, Generic, Optional, Tuple, TypeVar

V = TypeVar("V")


class CacheVersionado(Generic[V]):
    def __init__(self, nome: str):
        self._nome = nome
        self._armazenamento: Dict[Tuple[str, str], V] = {}
        self._lock = Lock()
        self._locks_por_chave: Dict[Tuple[str, str], Lock] = {}

    def obter(self, fingerprint: str, versao: str) -> Optional[V]:
        with self._lock:
            return self._armazenamento.get((fingerprint, versao))

    def guardar(self, fingerprint: str, versao: str, valor: V) -> None:
        with self._lock:
            self._armazenamento[(fingerprint, versao)] = valor

    def _lock_da_chave(self, chave: Tuple[str, str]) -> Lock:
        # Lock global só para o instante de criar/obter o lock desta
        # chave — nunca para segurar durante calcular() (isso é
        # exactamente o que causaria o bloqueio entre documentos
        # independentes que a Fase 11 quer evitar).
        with self._lock:
            lock_existente = self._locks_por_chave.get(chave)
            if lock_existente is None:
                lock_existente = Lock()
                self._locks_por_chave[chave] = lock_existente
            return lock_existente

    def obter_ou_calcular(
        self, fingerprint: str, versao: str, calcular: Callable[[], V]
    ) -> Tuple[V, bool]:
        """Devolve (valor, veio_do_cache). `calcular` só é chamado em
        caso de cache miss — e no máximo uma vez por chave, mesmo com
        pedidos concorrentes (ver nota da Fase 11 no topo do módulo)."""
        chave = (fingerprint, versao)

        existente = self.obter(fingerprint, versao)
        if existente is not None:
            return existente, True

        # Só a partir daqui se serializa por CHAVE — pedidos para outras
        # chaves nunca esperam por este lock.
        with self._lock_da_chave(chave):
            # Double-check: outro pedido pode ter calculado e gravado
            # enquanto esperávamos por este lock.
            existente = self.obter(fingerprint, versao)
            if existente is not None:
                return existente, True

            valor = calcular()
            self.guardar(fingerprint, versao, valor)
            return valor, False
