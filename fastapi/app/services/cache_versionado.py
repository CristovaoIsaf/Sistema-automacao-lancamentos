"""
Cache em memória, versionado, por fingerprint — mecanismo genérico
reutilizado pelas Fases 5 (cache de OCR, ver ocr_service.py) e 6 (cache
de validação, ver document_validation.py). Uma só implementação em vez de
duas quase iguais (ver secção 6 do mapa de impacto: não duplicar regras/
mecanismos entre ficheiros).

Cada entrada é indexada por (fingerprint, versão) — não só pelo
fingerprint. Isto significa que, se a versão da lógica que produziu o
valor mudar (ex: nova versão do motor de validação, ou do idioma/
pré-processamento do OCR), uma entrada antiga nunca é devolvida como se
fosse actual: é tratada como cache miss e recalculada, tal como pedido no
mapa de impacto ("Quando uma dessas versões tornar o resultado
incompatível, não reutilizar cegamente o cache antigo").

Em memória de processo (sem Redis/BD) — suficiente para o âmbito actual
do projecto (um único processo FastAPI) e não introduz nenhuma
dependência nova (ver secção 14: não introduzir bibliotecas/serviços sem
verificar primeiro se já existe solução equivalente — não existe). Se um
dia for preciso partilhar entre vários processos/réplicas, troca-se só a
implementação interna, sem alterar esta interface.
"""

from threading import Lock
from typing import Callable, Dict, Generic, Optional, Tuple, TypeVar

V = TypeVar("V")


class CacheVersionado(Generic[V]):
    def __init__(self, nome: str):
        self._nome = nome
        self._armazenamento: Dict[Tuple[str, str], V] = {}
        self._lock = Lock()

    def obter(self, fingerprint: str, versao: str) -> Optional[V]:
        with self._lock:
            return self._armazenamento.get((fingerprint, versao))

    def guardar(self, fingerprint: str, versao: str, valor: V) -> None:
        with self._lock:
            self._armazenamento[(fingerprint, versao)] = valor

    def obter_ou_calcular(
        self, fingerprint: str, versao: str, calcular: Callable[[], V]
    ) -> Tuple[V, bool]:
        """Devolve (valor, veio_do_cache). `calcular` só é chamado em
        caso de cache miss."""
        existente = self.obter(fingerprint, versao)
        if existente is not None:
            return existente, True

        valor = calcular()
        self.guardar(fingerprint, versao, valor)
        return valor, False
