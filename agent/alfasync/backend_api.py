"""Cliente das rotas /api/v1/access/agent/** do AlfaSchool.

Duas ideias sustentam este arquivo:

1. **Classificacao do erro.** Rede caiu, DNS falhou, servidor devolveu 5xx, 429 ou
   401: e transitorio, vale repetir. 4xx que nao seja 401/429 e recusa definitiva —
   repetir nunca vai mudar a resposta. Sem essa distincao, a fila de eventos trava
   para sempre no primeiro evento malformado, porque ela e ordenada: o item ruim
   fica na frente e nenhum acesso posterior sobe. Foi um bug real em producao.

2. **Tolerancia a backend mais novo ou mais velho.** Se uma rota responder 404, o
   gateway loga WARN uma vez por rota e segue o ciclo. Ele nao pode morrer porque o
   servidor ainda nao subiu aquele endpoint — o leitor continua registrando gente
   passando e a fila continua guardando.

As respostas vem embrulhadas em {"timestamp", "status", "message", "data"}; tudo
que sai daqui ja vem desembrulhado.
"""
import logging
import socket
import threading
from typing import Any, Dict, List, Optional, Tuple

import requests

logger = logging.getLogger(__name__)

PREFIXO = "/api/v1/access/agent"

# 4xx que ainda valem retentativa. 401 porque a reautenticacao pode resolver;
# 429 porque e o servidor pedindo calma, nao recusando o dado.
STATUS_4XX_TRANSITORIOS = {401, 429}


class BackendApiError(Exception):
    """Falha ao falar com o backend.

    `transitorio=True` significa "tente de novo depois"; False significa
    "o servidor recusou este dado, repetir e desperdicio".
    """

    def __init__(self, mensagem: str, status_code: Optional[int] = None,
                 transitorio: bool = True, detalhe: str = "",
                 rota_ausente: bool = False):
        super().__init__(mensagem)
        self.status_code = status_code
        self.transitorio = transitorio
        self.detalhe = detalhe
        self.rota_ausente = rota_ausente

    @property
    def definitivo(self) -> bool:
        return not self.transitorio


def classificar_status(status: Optional[int]) -> bool:
    """True quando vale repetir. Regra unica do gateway, usada tambem pela fila."""
    if status is None:
        return True          # erro de rede: sempre transitorio
    if status >= 500:
        return True
    if status == 404:
        # Rota ausente (backend mais velho) e indistinguivel aqui de recurso
        # inexistente. Tratar como transitorio evita descartar acessos so porque
        # o endpoint ainda nao subiu; a fila limita as tentativas por conta propria.
        return True
    if 400 <= status < 500:
        return status in STATUS_4XX_TRANSITORIOS
    return True


def classificar_erro_de_rede(exc: BaseException) -> Tuple[str, str]:
    """(categoria, frase curta) para log legivel: distingue LAN de nuvem."""
    s = str(exc)
    baixo = s.lower()
    if isinstance(exc, requests.exceptions.Timeout):
        return ("timeout", "timeout aguardando resposta")
    if isinstance(exc, requests.exceptions.HTTPError):
        status = getattr(getattr(exc, "response", None), "status_code", "?")
        return ("http", f"HTTP {status}")
    if isinstance(exc, requests.exceptions.ConnectionError):
        if "getaddrinfo" in baixo or "name or service not known" in baixo or "nameresolution" in baixo:
            return ("dns", "DNS nao resolveu")
        if "no route to host" in baixo or "unreachable" in baixo or "10065" in baixo:
            return ("rota", "rota indisponivel (a rede pode estar fora)")
        if "connection refused" in baixo:
            return ("conexao", "conexao recusada")
        if "connection reset" in baixo or "connection aborted" in baixo:
            return ("conexao", "conexao interrompida pelo servidor")
        return ("conexao", "falha de conexao")
    if isinstance(exc, socket.gaierror):
        return ("dns", "DNS nao resolveu")
    return ("desconhecido", s[:200])


def desembrulhar(corpo: Any) -> Any:
    """Tira o `data` do envelope {"timestamp","status","message","data"}.

    Aceita resposta crua tambem: backend antigo (ou uma rota que ainda nao
    padronizou) devolve o objeto direto, e isso nao pode quebrar o gateway.
    """
    if isinstance(corpo, dict) and "data" in corpo and "status" in corpo:
        return corpo.get("data")
    return corpo


def mensagem_do_servidor(resposta, limite: int = 400) -> str:
    """A frase que o servidor mandou junto do erro — e o que o operador precisa ler.

    Nada aqui pode levantar excecao: estamos no caminho de erro. Pagina HTML de
    proxy fica de fora, e ruido.
    """
    if resposta is None:
        return ""
    try:
        tipo = str(resposta.headers.get("Content-Type", "")).lower()
        if "json" in tipo:
            corpo = resposta.json()
            if isinstance(corpo, dict):
                partes = []
                for chave in ("message", "error", "details", "detail"):
                    valor = corpo.get(chave)
                    if isinstance(valor, str) and valor.strip() and valor not in partes:
                        partes.append(valor.strip())
                texto = " — ".join(partes)
            else:
                texto = str(corpo)
        else:
            texto = resposta.text or ""
            if texto.lstrip().startswith("<"):
                return ""
        texto = " ".join(texto.split())
        return texto[:limite - 1] + "…" if len(texto) > limite else texto
    except Exception:
        return ""


class BackendApi:
    def __init__(self, base_url: str, client_id: str, username: str, password: str,
                 timeout: float = 30.0, verify_ssl: bool = True,
                 agent_version: str = "0.1.0"):
        self.base_url = base_url.rstrip("/")
        self.client_id = client_id
        self.username = username
        self.password = password
        self.timeout = timeout
        self.verify_ssl = verify_ssl
        self.agent_version = agent_version

        self.token: Optional[str] = None
        self._sessao = requests.Session()
        self._lock = threading.Lock()   # protege o token entre as threads do laco
        # Rotas que ja responderam 404: uma linha de WARN por rota, nao uma por ciclo.
        # Com 5s de polling, logar sempre encheria o disco da maquina da escola.
        self._rotas_ausentes: set = set()

    # ─── infraestrutura ───────────────────────────────────────────────────────

    def _url(self, rota: str) -> str:
        return f"{self.base_url}{PREFIXO}{rota}"

    def _cabecalhos(self) -> Dict[str, str]:
        with self._lock:
            token = self.token
        if not token:
            self.autenticar()
            with self._lock:
                token = self.token
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    def autenticar(self) -> str:
        """POST /agent/login → token. Levanta BackendApiError se nao vier token."""
        url = self._url("/login")
        corpo = {
            "clientId": self.client_id,
            "username": self.username,
            "password": self.password,
        }
        try:
            resposta = self._sessao.post(url, json=corpo, timeout=self.timeout,
                                         verify=self.verify_ssl)
        except requests.RequestException as e:
            categoria, frase = classificar_erro_de_rede(e)
            raise BackendApiError(f"Login no backend falhou [{categoria}]: {frase}",
                                  transitorio=True)

        if resposta.status_code >= 400:
            detalhe = mensagem_do_servidor(resposta)
            raise BackendApiError(
                f"Login recusado pelo backend (HTTP {resposta.status_code}): {detalhe}",
                status_code=resposta.status_code,
                transitorio=classificar_status(resposta.status_code),
                detalhe=detalhe,
            )

        try:
            dados = desembrulhar(resposta.json()) or {}
        except ValueError:
            raise BackendApiError("Login devolveu corpo que nao e JSON.", transitorio=True)

        token = None
        if isinstance(dados, dict):
            # Grafias aceitas porque o lado servidor esta sendo escrito em paralelo.
            for chave in ("token", "accessToken", "access_token", "jwt"):
                if dados.get(chave):
                    token = str(dados[chave])
                    break
        if not token:
            raise BackendApiError(
                "Login respondeu 200 mas sem token no corpo (esperado data.token).",
                status_code=resposta.status_code, transitorio=True,
            )

        with self._lock:
            self.token = token
        logger.info("Autenticado no backend.")
        return token

    def _requisitar(self, metodo: str, rota: str, *, params: Optional[dict] = None,
                    json_body: Optional[dict] = None, timeout: Optional[float] = None,
                    tolerar_404: bool = False, _reautenticou: bool = False) -> Any:
        """Chamada autenticada, com uma reautenticacao automatica em 401.

        `tolerar_404=True` devolve None em vez de levantar — para as rotas de
        leitura (devices, users, tasks), que um backend mais velho pode nao ter.
        """
        url = self._url(rota)
        try:
            resposta = self._sessao.request(
                metodo, url, headers=self._cabecalhos(), params=params, json=json_body,
                timeout=timeout or self.timeout, verify=self.verify_ssl,
            )
        except requests.RequestException as e:
            categoria, frase = classificar_erro_de_rede(e)
            raise BackendApiError(f"{metodo.upper()} {rota} falhou [{categoria}]: {frase}",
                                  transitorio=True)

        if resposta.status_code == 401 and not _reautenticou:
            # Token expirou ou o backend reiniciou: pega outro e repete uma vez.
            logger.debug("401 do backend — reautenticando.")
            with self._lock:
                self.token = None
            self.autenticar()
            return self._requisitar(metodo, rota, params=params, json_body=json_body,
                                    timeout=timeout, tolerar_404=tolerar_404,
                                    _reautenticou=True)

        if resposta.status_code == 404:
            if rota not in self._rotas_ausentes:
                self._rotas_ausentes.add(rota)
                logger.warning(
                    f"Backend respondeu 404 em {PREFIXO}{rota} — rota ausente nesta "
                    "versao do servidor. Seguindo o ciclo; tentarei de novo depois."
                )
            if tolerar_404:
                return None
            raise BackendApiError(
                f"{metodo.upper()} {rota}: HTTP 404",
                status_code=404, transitorio=True, rota_ausente=True,
            )

        if resposta.status_code >= 400:
            detalhe = mensagem_do_servidor(resposta)
            raise BackendApiError(
                f"{metodo.upper()} {rota}: HTTP {resposta.status_code} {detalhe}".strip(),
                status_code=resposta.status_code,
                transitorio=classificar_status(resposta.status_code),
                detalhe=detalhe,
            )

        # Rota que voltou a existir: permite um novo WARN se sumir de novo.
        self._rotas_ausentes.discard(rota)

        if resposta.status_code == 204 or not (resposta.content or b"").strip():
            return None
        try:
            return desembrulhar(resposta.json())
        except ValueError:
            return None

    # ─── rotas ────────────────────────────────────────────────────────────────

    def listar_dispositivos(self) -> List[Dict[str, Any]]:
        """GET /agent/devices — equipamentos desta unidade. Lista vazia se a rota nao existe."""
        dados = self._requisitar("get", "/devices", tolerar_404=True, timeout=15)
        if dados is None:
            return []
        if isinstance(dados, dict):
            dados = dados.get("content") or dados.get("devices") or []
        return [d for d in dados if isinstance(d, dict)]

    def listar_pessoas(self, atualizadas_apos: Optional[int] = None,
                       pagina: int = 0, tamanho: int = 200) -> Tuple[List[Dict[str, Any]], bool]:
        """GET /agent/users?updatedAfter= — delta paginado.

        Devolve (pessoas, tem_mais). `tem_mais` vem do envelope de pagina quando o
        backend manda um; sem isso, deduz por "a pagina veio cheia".
        """
        params: Dict[str, Any] = {"page": pagina, "size": tamanho}
        if atualizadas_apos is not None:
            params["updatedAfter"] = atualizadas_apos
        dados = self._requisitar("get", "/users", params=params, tolerar_404=True, timeout=60)
        if dados is None:
            return [], False
        if isinstance(dados, dict):
            pessoas = dados.get("content") or dados.get("users") or dados.get("items") or []
            total_paginas = dados.get("totalPages")
            if isinstance(total_paginas, int):
                return list(pessoas), (pagina + 1) < total_paginas
            if isinstance(dados.get("last"), bool):
                return list(pessoas), not dados["last"]
            return list(pessoas), len(pessoas) >= tamanho
        pessoas = [p for p in dados if isinstance(p, dict)]
        return pessoas, len(pessoas) >= tamanho

    def enviar_evento(self, evento: Dict[str, Any]) -> Any:
        """POST /agent/events — uma passagem. Levanta BackendApiError classificado."""
        return self._requisitar("post", "/events", json_body=evento)

    def enviar_heartbeat(self, corpo: Dict[str, Any]) -> Any:
        """POST /agent/heartbeat — status e versao. Nunca derruba o laco."""
        corpo = dict(corpo)
        corpo.setdefault("clientId", self.client_id)
        corpo.setdefault("agentVersion", self.agent_version)
        return self._requisitar("post", "/heartbeat", json_body=corpo,
                                tolerar_404=True, timeout=15)

    def tarefas_pendentes(self) -> List[Dict[str, Any]]:
        """GET /agent/tasks/pending — comandos que o backend quer executados aqui."""
        dados = self._requisitar("get", "/tasks/pending", tolerar_404=True, timeout=15)
        if dados is None:
            return []
        if isinstance(dados, dict):
            dados = dados.get("content") or dados.get("tasks") or []
        return [t for t in dados if isinstance(t, dict)]

    def responder_tarefa(self, task_id: Any, sucesso: bool,
                         erro: str = "", extra: Optional[Dict[str, Any]] = None) -> None:
        """PUT /agent/tasks/{id}/result — devolve o resultado.

        Falha aqui nao pode derrubar o worker: a tarefa volta como pendente no
        proximo ciclo, e reexecutar `sincronizar_pessoa` ou `remover_pessoa` e
        inofensivo (ambas sao idempotentes no leitor).
        """
        corpo: Dict[str, Any] = {"success": bool(sucesso)}
        if erro:
            corpo["error"] = erro[:500]
        if extra:
            corpo.update(extra)
        try:
            self._requisitar("put", f"/tasks/{task_id}/result", json_body=corpo,
                             tolerar_404=True, timeout=30)
        except BackendApiError as e:
            logger.warning(f"Falha ao devolver o resultado da tarefa {task_id}: {e}")
