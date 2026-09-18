"""Estado local do gateway (state.json), por equipamento.

Guarda duas coisas:

* `last_event_id`: ate onde ja lemos o historico de cada leitor. Sem isso, um
  reinicio do gateway reimportaria todo o access_logs do equipamento.
* `fotos`: hash da foto ja gravada em cada leitor, por pessoa. Sem isso, cada
  ciclo de sincronizacao reenviaria a mesma imagem para o leitor — trinta
  segundos de rede e CPU do equipamento gastos a toa, e o leitor fica lento na
  hora da entrada, que e exatamente quando ele precisa responder.

E guarda o cursor do delta de pessoas (`updatedAfter`).

O arquivo e gravado de forma atomica (tmp + os.replace): queda de energia no meio
da gravacao nao pode deixar um state.json truncado, que faria o gateway comecar
do zero.
"""
import json
import logging
import os
import threading
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)


class State:
    def __init__(self, caminho: str):
        self.caminho = caminho
        self._lock = threading.RLock()
        self._dados: Dict[str, Any] = {"devices": {}, "users_updated_after": None}
        self._carregar()

    # ─── persistencia ─────────────────────────────────────────────────────────

    def _carregar(self) -> None:
        if not os.path.exists(self.caminho):
            return
        try:
            with open(self.caminho, "r", encoding="utf-8") as fh:
                dados = json.load(fh)
            if isinstance(dados, dict):
                self._dados["devices"] = dados.get("devices") or {}
                self._dados["users_updated_after"] = dados.get("users_updated_after")
        except Exception as e:
            # State corrompido nao pode impedir o gateway de subir: sem ele o
            # leitor e reimportado, o que e chato, mas nao perde passagem nenhuma
            # (o backend deduplica pelo par dispositivo + id do log).
            logger.warning(f"state.json ilegivel ({e}) — comecando com estado vazio.")

    def salvar(self) -> None:
        with self._lock:
            copia = json.dumps(self._dados, ensure_ascii=False, indent=2)
        try:
            pasta = os.path.dirname(os.path.abspath(self.caminho))
            if pasta:
                os.makedirs(pasta, exist_ok=True)
            temporario = self.caminho + ".tmp"
            with open(temporario, "w", encoding="utf-8") as fh:
                fh.write(copia)
                fh.flush()
                os.fsync(fh.fileno())
            os.replace(temporario, self.caminho)   # atomico no POSIX e no Windows
        except Exception as e:
            logger.warning(f"Falha ao gravar {self.caminho}: {e}")

    def _device(self, device_id: str) -> Dict[str, Any]:
        with self._lock:
            devices = self._dados.setdefault("devices", {})
            return devices.setdefault(str(device_id), {"last_event_id": 0, "fotos": {}})

    # ─── cursor de eventos ────────────────────────────────────────────────────

    def last_event_id(self, device_id: str) -> int:
        return int(self._device(device_id).get("last_event_id", 0) or 0)

    def set_last_event_id(self, device_id: str, event_id: int) -> None:
        with self._lock:
            self._device(device_id)["last_event_id"] = int(event_id)

    def detectar_reinicio_contador(self, device_id: str, event_id: int) -> bool:
        """BUG CLASSICO: o contador de eventos do leitor reinicia.

        Quando o equipamento e formatado, tem o historico limpo ou troca de
        firmware, `access_logs.id` volta a contar do 1. O cursor guardado aqui
        continua em, digamos, 48.312 — e a partir dai TODO evento novo chega com
        id menor que o cursor. O gateway que so aceita "id > cursor" simplesmente
        para de enviar passagem, para sempre, em silencio: o leitor funciona, o
        log nao acusa erro, e ninguem descobre ate alguem reclamar que a lista de
        presenca esta vazia ha uma semana.

        A regra aqui: id MENOR que o ultimo visto = o historico foi limpo. Zera o
        cursor e reimporta. Reenvio nao e problema — o ext_id leva o epoch do
        acesso junto do id do log, entao a fila nao colide, e o backend deduplica.

        Devolve True quando detectou e zerou.
        """
        atual = self.last_event_id(device_id)
        if atual > 0 and int(event_id) < atual:
            logger.warning(
                f"Equipamento {device_id}: contador de eventos reiniciado "
                f"(chegou id {event_id}, cursor estava em {atual}). "
                "O historico do leitor foi limpo — reimportando do inicio."
            )
            self.set_last_event_id(device_id, 0)
            self.salvar()
            return True
        return False

    # ─── fotos ────────────────────────────────────────────────────────────────

    def hash_da_foto(self, device_id: str, pessoa_id: Any) -> Optional[str]:
        return self._device(device_id).get("fotos", {}).get(str(pessoa_id))

    def foto_ja_enviada(self, device_id: str, pessoa_id: Any, hash_foto: str) -> bool:
        return bool(hash_foto) and self.hash_da_foto(device_id, pessoa_id) == hash_foto

    def registrar_foto(self, device_id: str, pessoa_id: Any, hash_foto: str) -> None:
        with self._lock:
            self._device(device_id).setdefault("fotos", {})[str(pessoa_id)] = hash_foto

    def esquecer_foto(self, device_id: str, pessoa_id: Any) -> None:
        """Chamado ao remover a pessoa: sem isso, um recadastro nao reenviaria a face."""
        with self._lock:
            self._device(device_id).get("fotos", {}).pop(str(pessoa_id), None)

    def esquecer_dispositivo(self, device_id: str) -> None:
        with self._lock:
            self._dados.get("devices", {}).pop(str(device_id), None)

    # ─── cursor do delta de pessoas ───────────────────────────────────────────

    @property
    def users_updated_after(self) -> Optional[int]:
        with self._lock:
            return self._dados.get("users_updated_after")

    @users_updated_after.setter
    def users_updated_after(self, valor: Optional[int]) -> None:
        with self._lock:
            self._dados["users_updated_after"] = valor


def hash_foto(conteudo: Any) -> str:
    """Hash estavel da foto, para comparar entre ciclos."""
    import hashlib

    if conteudo is None:
        return ""
    if isinstance(conteudo, str):
        conteudo = conteudo.encode("utf-8", "ignore")
    return hashlib.sha256(conteudo).hexdigest()
