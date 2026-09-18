"""Cliente HTTP do leitor facial Control iD, na rede local da escola.

O protocolo e simples e cheio de armadilhas:

* `/login.fcgi` devolve {"session": "..."} e essa sessao vai na QUERY STRING de
  toda chamada seguinte. Fazer login a cada operacao derruba o desempenho do
  leitor (ele guarda poucas sessoes simultaneas e comeca a recusar). Por isso a
  sessao fica em cache com TTL e so e refeita quando expira ou quando o leitor
  responde 401/403.
* Todo request tem timeout explicito. Um leitor mudo — cabo solto, fonte
  queimada, IP trocado pelo roteador — nao pode segurar o ciclo inteiro: o
  gateway atende varios equipamentos e precisa continuar coletando dos outros.
* O corpo vai sempre por `json.dumps`. Montar JSON concatenando string quebra no
  primeiro aluno chamado "D'Avila" ou com aspas no nome, e o erro so aparece em
  producao, no cadastro de uma pessoa especifica.

Este modulo nao decide nada: ele le eventos e executa comandos. Quem autoriza e
o backend.
"""
import base64
import json
import logging
import threading
import time
import urllib.parse
from typing import Any, Dict, List, Optional, Tuple

import requests

from backend_api import classificar_erro_de_rede

logger = logging.getLogger(__name__)


class DeviceApiError(Exception):
    def __init__(self, mensagem: str, status_code: Optional[int] = None,
                 corpo: str = ""):
        super().__init__(mensagem)
        self.status_code = status_code
        self.corpo = corpo


class ControlIdDevice:
    """Um equipamento. Uma instancia por leitor, compartilhada entre as threads."""

    def __init__(self, host: str, port: int = 80, username: str = "admin",
                 password: str = "", timeout: float = 8.0, sessao_ttl: float = 300.0,
                 nome: str = ""):
        self.base_url = f"http://{host}:{port}"
        self.host = host
        self.nome = nome or host
        self.username = username
        self.password = password
        self.timeout = timeout
        self.sessao_ttl = sessao_ttl

        self._sessao_token: Optional[str] = None
        self._sessao_expira_em: float = 0.0
        # Lock reentrante: heartbeat, coleta de eventos e execucao de tarefas
        # rodam em threads diferentes e compartilham este objeto (mesma sessao,
        # mesmo leitor). Sem serializar, um `create_objects` no meio de um
        # `user_set_image` corrompe o cadastro da face — so uma das duas "cola".
        self._lock = threading.RLock()
        self._http = requests.Session()

    # ─── sessao ───────────────────────────────────────────────────────────────

    def _sessao_valida(self) -> bool:
        return bool(self._sessao_token) and time.time() < self._sessao_expira_em

    def invalidar_sessao(self) -> None:
        with self._lock:
            self._sessao_token = None
            self._sessao_expira_em = 0.0

    def _garantir_sessao(self) -> str:
        with self._lock:
            if self._sessao_valida():
                return str(self._sessao_token)
            return self._login()

    def _login(self) -> str:
        url = f"{self.base_url}/login.fcgi"
        corpo = json.dumps({"login": self.username, "password": self.password})
        try:
            resposta = self._http.post(
                url, data=corpo, headers={"Content-Type": "application/json"},
                timeout=self.timeout,
            )
        except requests.RequestException as e:
            categoria, frase = classificar_erro_de_rede(e)
            raise DeviceApiError(f"Sem conexao com {self.nome} ({self.host}) [{categoria}]: {frase}")

        if resposta.status_code >= 400:
            raise DeviceApiError(
                f"{self.nome}: login recusado pelo leitor (HTTP {resposta.status_code}). "
                "Confira usuario e senha do equipamento.",
                status_code=resposta.status_code,
                corpo=(resposta.text or "")[:200],
            )
        try:
            token = (resposta.json() or {}).get("session")
        except ValueError:
            raise DeviceApiError(f"{self.nome}: login devolveu corpo que nao e JSON.")
        if not token:
            raise DeviceApiError(f"{self.nome}: login sem 'session' na resposta.")

        self._sessao_token = str(token)
        # Renova um pouco antes do TTL configurado: evita a corrida entre "faltam
        # 2s para expirar" e a chamada que sai agora.
        self._sessao_expira_em = time.time() + max(30.0, self.sessao_ttl - 15.0)
        logger.debug(f"{self.nome}: sessao aberta no leitor.")
        return self._sessao_token

    # ─── chamada generica ────────────────────────────────────────────────────

    def _post(self, endpoint: str, corpo: Optional[Dict[str, Any]] = None,
              timeout: Optional[float] = None, tolerar_400: bool = False,
              _repetiu: bool = False) -> Dict[str, Any]:
        """POST autenticado em um .fcgi. A sessao vai na query string."""
        with self._lock:
            token = self._garantir_sessao()
            url = f"{self.base_url}{endpoint}?" + urllib.parse.urlencode({"session": token})
            dados = json.dumps(corpo if corpo is not None else {})
            try:
                resposta = self._http.post(
                    url, data=dados, headers={"Content-Type": "application/json"},
                    timeout=timeout or self.timeout,
                )
            except requests.RequestException as e:
                categoria, frase = classificar_erro_de_rede(e)
                raise DeviceApiError(
                    f"{self.nome}: falha em {endpoint} [{categoria}]: {frase}"
                )

            if resposta.status_code in (401, 403) and not _repetiu:
                # Sessao morreu antes do TTL (reinicio do leitor, limpeza interna).
                # Uma unica retentativa com sessao nova; mais que isso mascara
                # credencial errada em loop.
                logger.debug(f"{self.nome}: sessao expirada no leitor — refazendo login.")
                self.invalidar_sessao()
                return self._post(endpoint, corpo, timeout=timeout,
                                  tolerar_400=tolerar_400, _repetiu=True)

            texto = (resposta.text or "")[:500]
            if resposta.status_code == 400:
                # O firmware usa 400 tanto para "ja existe" quanto para "payload
                # rejeitado". Quem chama decide; aqui so devolvemos o corpo.
                if not tolerar_400:
                    logger.warning(f"{self.nome}: HTTP 400 em {endpoint}: {texto}")
                return {"http_400": True, "corpo": texto}

            if resposta.status_code >= 400:
                raise DeviceApiError(
                    f"{self.nome}: HTTP {resposta.status_code} em {endpoint}",
                    status_code=resposta.status_code, corpo=texto,
                )

            if not (resposta.content or b"").strip():
                return {}
            try:
                resultado = resposta.json()
            except ValueError:
                return {}
            return resultado if isinstance(resultado, dict) else {"resultado": resultado}

    # ─── leitura ──────────────────────────────────────────────────────────────

    def ping(self, timeout: float = 3.0) -> Tuple[bool, Optional[int]]:
        """Sonda leve, em conexao propria: nao segura o lock do equipamento.

        Devolve (alcancavel, milissegundos). Usada pelo heartbeat para dizer ao
        backend se o leitor esta de pe.
        """
        inicio = time.time()
        try:
            requests.get(self.base_url, timeout=timeout)
            return True, int((time.time() - inicio) * 1000)
        except requests.RequestException:
            return False, None

    def numero_de_serie(self) -> Optional[str]:
        try:
            info = self._post("/system_information.fcgi", {}, timeout=self.timeout)
        except DeviceApiError:
            return None
        for chave in ("serial", "serial_number", "device_id"):
            if info.get(chave):
                return str(info[chave])
        return None

    def listar_eventos(self, desde_id: int = 0, limite: int = 200) -> List[Dict[str, Any]]:
        """access_logs com id >= desde_id.

        O filtro e por id, nunca por `time`: o firmware aplica o `limit` antes de
        qualquer ordenacao, entao filtrar por horario devolve sempre os N logs
        mais antigos e os eventos novos nunca aparecem depois que o historico
        interno passa do limite.

        `desde_id` e inclusivo de proposito (implementado como `> desde_id - 1`,
        o unico operador confirmado no firmware): o ultimo log ja processado volta
        no lote como sentinela. Lote vazio com desde_id > 0 significa que aquele
        log sumiu — historico apagado (ver state.detectar_reinicio_contador).
        """
        corpo = {
            "object": "access_logs",
            "fields": ["id", "user_id", "time", "event", "device_id"],
            "where": {"access_logs": {"id": {">": int(desde_id) - 1}}},
            "limit": int(limite),
        }
        resultado = self._post("/load_objects.fcgi", corpo)
        eventos = resultado.get("access_logs", [])
        return [e for e in eventos if isinstance(e, dict)]

    def contar_usuarios(self) -> int:
        resultado = self._post("/load_objects.fcgi", {"object": "users", "limit": 1000})
        return len(resultado.get("users", []))

    # ─── escrita ──────────────────────────────────────────────────────────────

    def criar_usuario(self, user_id: int, nome: str, matricula: str = "") -> Dict[str, Any]:
        """Cria (ou recria) a pessoa no leitor com o id que o backend mandou.

        O id vem do backend de proposito: e ele que casa o evento do leitor com o
        aluno. `create_objects` com id ja existente devolve 400 — tratado como
        "ja existe", nao como erro.
        """
        corpo = {
            "object": "users",
            "values": [{
                "id": int(user_id),
                "name": str(nome or "")[:80],
                "registration": str(matricula or user_id),
            }],
        }
        return self._post("/create_objects.fcgi", corpo, tolerar_400=True)

    def vincular_grupo(self, user_id: int, group_id: int = 1) -> Dict[str, Any]:
        """Poe a pessoa no grupo padrao do leitor (o que tem a regra de acesso).

        Sem grupo, o leitor reconhece o rosto e nega a passagem. Isto NAO e o
        gateway decidindo autorizacao: quem decide se a pessoa existe no leitor e
        o backend, que manda (ou nao manda) a tarefa `sincronizar_pessoa`.
        """
        corpo = {
            "object": "user_groups",
            "values": [{"user_id": int(user_id), "group_id": int(group_id)}],
        }
        # 400 aqui e quase sempre UNIQUE constraint = ja estava no grupo.
        return self._post("/create_objects.fcgi", corpo, tolerar_400=True)

    def enviar_foto(self, user_id: int, foto_base64: str,
                    timeout: float = 30.0) -> Dict[str, Any]:
        """Envia a face para /user_set_image.fcgi (corpo binario, nao JSON).

        A imagem vai como o backend mandou, sem reprocessar: o gateway nao carrega
        biblioteca de imagem (dependencia minima). Se o leitor recusar a foto
        ("Face too distant", rosto nao encontrado), a recusa volta como
        DeviceApiError e vira o erro da tarefa — quem corrige a foto e o cadastro,
        no backend.
        """
        try:
            binario = base64.b64decode(foto_base64, validate=False)
        except Exception as e:
            raise DeviceApiError(f"{self.nome}: foto em base64 invalida para a pessoa {user_id}: {e}")
        if not binario:
            raise DeviceApiError(f"{self.nome}: foto vazia para a pessoa {user_id}.")

        with self._lock:
            token = self._garantir_sessao()
            params = urllib.parse.urlencode({
                "session": token,
                "user_id": int(user_id),
                "timestamp": int(time.time()),
            })
            url = f"{self.base_url}/user_set_image.fcgi?{params}"
            try:
                resposta = self._http.post(
                    url, data=binario,
                    headers={"Content-Type": "application/octet-stream"},
                    timeout=timeout,
                )
            except requests.RequestException as e:
                categoria, frase = classificar_erro_de_rede(e)
                raise DeviceApiError(f"{self.nome}: falha ao enviar foto [{categoria}]: {frase}")

            if resposta.status_code in (401, 403):
                self.invalidar_sessao()
                raise DeviceApiError(f"{self.nome}: sessao recusada ao enviar foto.",
                                     status_code=resposta.status_code)
            if resposta.status_code >= 400:
                raise DeviceApiError(
                    f"{self.nome}: leitor recusou a foto (HTTP {resposta.status_code})",
                    status_code=resposta.status_code, corpo=(resposta.text or "")[:300],
                )
            try:
                corpo = resposta.json() or {}
            except ValueError:
                corpo = {}
            if corpo.get("success") is False:
                erros = corpo.get("errors") or []
                motivo = erros[0].get("message", "recusada") if erros else "recusada"
                raise DeviceApiError(f"{self.nome}: foto recusada pelo leitor: {motivo}")
            return corpo

    def remover_usuario(self, user_id: int) -> Dict[str, Any]:
        """Tira a pessoa do leitor (cadastro e face)."""
        corpo = {"object": "users", "where": {"users": {"id": int(user_id)}}}
        return self._post("/destroy_objects.fcgi", corpo, tolerar_400=True)

    def abrir_porta(self, door_id: int = 1) -> Dict[str, Any]:
        """Aciona o rele. `parameters` precisa ser "door=N".

        Usar `id=N` devolve HTTP 200 e NAO pulsa o rele — armadilha conhecida do
        firmware. Sem retentativa: se a resposta se perdeu depois de acionar, um
        retry abriria a porta duas vezes.
        """
        corpo = {"actions": [{"action": "door", "parameters": f"door={int(door_id)}"}]}
        return self._post("/execute_actions.fcgi", corpo)

    def fechar(self) -> None:
        try:
            self._http.close()
        except Exception:
            pass
