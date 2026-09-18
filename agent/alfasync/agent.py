"""AlfaSync School — gateway local.

Roda numa maquina DENTRO da escola. O backend esta na nuvem e nao alcanca a LAN
onde vivem os leitores faciais Control iD; o gateway alcanca os dois lados: fala
HTTP com os leitores na rede local e faz polling no backend.

Divisao de responsabilidade, sem excecao:
  o backend DECIDE, o gateway EXECUTA.
O gateway coleta a passagem e roda o comando que mandaram. Ele nunca decide
autorizacao por conta propria. (Regra offline — liberar acesso com a internet
caida — NAO esta implementada; seria uma etapa futura e mudaria essa divisao.)

O laco principal sao cinco threads com intervalos configuraveis:
  heartbeat (~5s) · coleta de eventos (~5s) · reenvio da fila (~15s)
  · sincronizacao de pessoas/faces (~30s) · tarefas pendentes (~10s)
"""
import json
import logging
import logging.handlers
import os
import signal
import sys
import threading
import time
from typing import Any, Callable, Dict, List, Optional, Tuple

import clock
from backend_api import BackendApi, BackendApiError
from config import Config, ConfigError, carregar, normalizar_device
from device_api import ControlIdDevice, DeviceApiError
from event_queue import EventQueue
from state import State, hash_foto

logger = logging.getLogger("alfasync")

VERSAO = "0.1.0"

# Eventos do firmware Control iD → vocabulario do backend.
# 5 e 6 sao negativas por regra de acesso; o metodo reportado nesses codigos nao
# e confiavel no firmware, entao vai nulo em vez de mentir na apuracao.
MAPA_EVENTOS = {
    1: ("access", "card"),
    2: ("access", "password"),
    3: ("access", "fingerprint"),
    4: ("access", "face"),
    5: ("denied", None),
    6: ("denied", None),
    7: ("access", "face"),
}

# Quantas tentativas um evento aguenta contra uma rota que responde 404 antes de
# ser descartado. 404 no POST /events pode ser backend mais velho (a rota ainda
# nao existe) ou payload que nao casa com nenhum equipamento la. No primeiro caso
# desistir cedo perderia acesso; no segundo, insistir para sempre trava a fila.
# ~10 tentativas com backoff cobrem um deploy demorado e depois desistem.
MAX_TENTATIVAS_ROTA_AUSENTE = 10

# Lotes vazios consecutivos antes de concluir que o historico do leitor foi
# apagado. Como o `desde_id` e inclusivo, o log do cursor volta no lote como
# sentinela: lote vazio com cursor > 0 significa que aquele log nao existe mais.
# Nao reagimos ao primeiro: um lote vazio pontual (leitor ocupado, resposta
# truncada) dispararia uma reimportacao do historico inteiro sem necessidade.
POLLS_VAZIOS_ANTES_DE_REINICIAR = 3


# ─── funcoes puras (testaveis sem rede) ──────────────────────────────────────

def montar_ext_id(device_id: Any, log_id: Any, epoch: Any) -> str:
    """Chave de idempotencia do evento.

    Inclui o epoch do acesso, e nao so o id do log: depois de um format do leitor
    os ids reiniciam do 1, e sem o epoch o acesso novo colidiria com um antigo
    ja enfileirado — a passagem de hoje seria silenciosamente ignorada pelo
    INSERT OR IGNORE.
    """
    return f"{device_id}:{log_id}:{epoch or 0}"


def montar_evento(device: Dict[str, Any], evento: Dict[str, Any],
                  client_id: str) -> Dict[str, Any]:
    """Traduz um access_log do leitor no corpo de POST /agent/events."""
    codigo = evento.get("event", 1)
    tipo, metodo = MAPA_EVENTOS.get(codigo, ("access", None))
    epoch = evento.get("time") or 0
    timestamp = clock.epoch_local_para_iso(epoch) if epoch else clock.agora_iso()
    user_id = evento.get("user_id")
    return {
        "externalEventId": montar_ext_id(device["id"], evento.get("id"), epoch),
        "clientId": client_id,
        "deviceId": str(device["id"]),
        "deviceLogId": evento.get("id"),
        "personId": str(user_id) if user_id else None,
        "eventType": tipo,
        "method": metodo,
        "rawEvent": codigo,
        "timestamp": timestamp,
    }


def drenar_fila(fila: EventQueue, enviar: Callable[[Dict[str, Any]], Any],
                limite: int = 50,
                max_tentativas_rota_ausente: int = MAX_TENTATIVAS_ROTA_AUSENTE,
                agora: Optional[float] = None) -> Tuple[int, int]:
    """Tenta entregar os eventos pendentes. Devolve (enviados, descartados).

    A politica de erro mora aqui, e e o coracao do "a fila nao pode travar":

    * recusa DEFINITIVA (4xx que nao seja 401/429/404): o evento sai da fila. Ele
      nunca vai ser aceito; deixa-lo na frente travaria toda a fila, que e
      ordenada — nenhuma passagem posterior subiria. Bug real em producao.
    * 404 (rota ausente ou equipamento desconhecido): conta a tentativa e SEGUE
      para os proximos eventos. Descarta so depois de N tentativas.
    * transitorio (rede, 5xx, 429, 401 que nem a reautenticacao resolveu):
      conta a tentativa e PARA o ciclo. O backend esta fora; martelar as outras
      linhas so gasta bateria do nobreak.
    """
    enviados = 0
    descartados = 0
    for linha in fila.proximos(limite=limite, agora=agora):
        try:
            payload = json.loads(linha["payload"])
        except Exception:
            # Payload ilegivel nao vai ficar legivel na proxima tentativa.
            fila.descartar(linha["id"], motivo=f"payload corrompido (id {linha['id']})")
            descartados += 1
            continue

        try:
            enviar(payload)
            fila.marcar_enviado(linha["id"])
            enviados += 1
        except BackendApiError as e:
            if getattr(e, "rota_ausente", False) or e.status_code == 404:
                if linha.get("attempts", 0) + 1 >= max_tentativas_rota_ausente:
                    fila.descartar(linha["id"], motivo=f"404 persistente: {e}")
                    descartados += 1
                else:
                    fila.marcar_falha(linha["id"], str(e), agora=agora)
                continue
            if e.definitivo:
                fila.descartar(linha["id"], motivo=str(e))
                descartados += 1
                continue
            fila.marcar_falha(linha["id"], str(e), agora=agora)
            break
        except Exception as e:  # pragma: no cover — rede ja vem como BackendApiError
            fila.marcar_falha(linha["id"], str(e), agora=agora)
            break
    return enviados, descartados


def normalizar_pessoa(bruto: Dict[str, Any]) -> Dict[str, Any]:
    """Aceita as grafias possiveis do backend (portugues/ingles, camel/snake)."""
    def pega(*nomes, padrao=None):
        for nome in nomes:
            if nome in bruto and bruto[nome] not in (None, ""):
                return bruto[nome]
        return padrao

    return {
        "id": pega("id", "personId", "pessoaId", "alunoId"),
        "nome": str(pega("nome", "name", "nomeCompleto", padrao="") or ""),
        "matricula": str(pega("matricula", "registration", "codigo", padrao="") or ""),
        "foto": pega("fotoBase64", "photoBase64", "foto", "photo", "faceBase64"),
        "ativo": bool(pega("ativo", "active", "enabled", padrao=True)),
        "atualizado_em": pega("updatedAt", "atualizadoEm", "updated_at"),
    }


def para_epoch_millis(valor: Any) -> Optional[int]:
    """Normaliza updatedAt (millis, segundos ou ISO) para epoch em milissegundos."""
    if valor is None:
        return None
    if isinstance(valor, (int, float)):
        numero = int(valor)
        # Heuristica: menos de 10^11 e segundos (ate o ano 5138 em ms seria maior).
        return numero * 1000 if numero < 100_000_000_000 else numero
    if isinstance(valor, str):
        texto = valor.strip().replace("Z", "+00:00")
        try:
            from datetime import datetime

            return int(datetime.fromisoformat(texto).timestamp() * 1000)
        except Exception:
            return None
    return None


def mesclar_dispositivos(do_config: List[Dict[str, Any]],
                         do_backend: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """A lista do backend manda; o config completa o que o backend nao sabe.

    O backend conhece o id do equipamento (que amarra o evento a unidade) mas
    frequentemente nao guarda a senha de administracao do leitor — e nem deveria.
    Casa por id e, se nao achar, por host.
    """
    resultado: Dict[str, Dict[str, Any]] = {}
    por_host = {d["host"]: d for d in do_config if d.get("host")}
    por_id = {d["id"]: d for d in do_config if d.get("id")}

    for bruto in do_backend:
        device = normalizar_device(bruto)
        local = por_id.get(device["id"]) or por_host.get(device["host"]) or {}
        for chave in ("host", "port", "username", "password", "door_id", "modelo"):
            if not device.get(chave) and local.get(chave):
                device[chave] = local[chave]
        # Senha do leitor: o config sempre ganha — e ele que o tecnico atualiza.
        if local.get("password"):
            device["password"] = local["password"]
        if device["host"]:
            resultado[device["id"]] = device
        else:
            logger.warning(
                f"Equipamento {device['id']} ({device['nome']}) veio do backend sem IP e "
                "nao esta no config.json — ignorado ate alguem informar o endereco na LAN."
            )

    # Equipamentos so no config (backend ainda sem a rota /devices, ou leitor novo)
    for device in do_config:
        if device["id"] and device["id"] not in resultado and device.get("ativo", True):
            resultado[device["id"]] = device
    return list(resultado.values())


def tipo_da_tarefa(tarefa: Dict[str, Any]) -> str:
    """Nome do comando, tolerante a grafia."""
    for chave in ("tipo", "type", "command", "comando", "acao", "action"):
        valor = tarefa.get(chave)
        if isinstance(valor, str) and valor.strip():
            return valor.strip().lower()
    return ""


def payload_da_tarefa(tarefa: Dict[str, Any]) -> Dict[str, Any]:
    for chave in ("payload", "parametros", "params", "data", "dados"):
        valor = tarefa.get(chave)
        if isinstance(valor, dict):
            return valor
        if isinstance(valor, str) and valor.strip().startswith("{"):
            try:
                carregado = json.loads(valor)
                if isinstance(carregado, dict):
                    return carregado
            except Exception:
                pass
    return tarefa


# ─── gateway ──────────────────────────────────────────────────────────────────

class Gateway:
    def __init__(self, config: Config):
        self.config = config
        self.backend = BackendApi(
            config.backend_url, config.client_id, config.username, config.password,
            timeout=config.timeout_backend, verify_ssl=config.verify_ssl,
            agent_version=VERSAO,
        )
        self.state = State(config.state_path)
        self.fila = EventQueue(config.fila_path, backoff_base=config.backoff_base_s,
                               backoff_teto=config.backoff_teto_s)

        self.devices: Dict[str, Dict[str, Any]] = {}
        self.apis: Dict[str, ControlIdDevice] = {}
        self._devices_lock = threading.RLock()

        self._parar = threading.Event()
        self._threads: List[threading.Thread] = []
        self._ultima_carga_devices = 0.0
        # Lotes vazios consecutivos por equipamento. So em memoria de proposito:
        # um restart recomeca a tolerancia, o que e o lado conservador do erro.
        self._polls_vazios: Dict[str, int] = {}

        # Quedas sao logadas por TRANSICAO, nunca por ciclo. Com polling de 5s,
        # uma noite de internet fora geraria dezenas de milhares de linhas iguais
        # e giraria o log rotativo, apagando justamente o que explica a falha.
        self._leitor_online: Dict[str, bool] = {}
        self._backend_ok: Optional[bool] = None
        self._backend_caiu_em: float = 0.0
        # Sem este lock, duas rotinas que descobrem a queda no mesmo instante
        # logam a transicao duas vezes.
        self._log_lock = threading.Lock()

    # ─── ciclo de vida ────────────────────────────────────────────────────────

    def iniciar(self) -> None:
        logger.info(f"AlfaSync School {VERSAO} — gateway local iniciando.")
        logger.info(f"Backend: {self.config.backend_url} | unidade: {self.config.client_id}")
        try:
            self.backend.autenticar()
        except BackendApiError as e:
            # Sem backend o gateway ainda serve: coleta do leitor e guarda na fila.
            logger.warning(f"Nao autenticou agora ({e}) — seguindo; tentarei a cada ciclo.")

        self._recarregar_dispositivos()

        tarefas = [
            ("heartbeat", self._worker_heartbeat, self.config.intervalo_heartbeat),
            ("eventos", self._worker_eventos, self.config.intervalo_eventos),
            ("fila", self._worker_fila, self.config.intervalo_fila),
            ("pessoas", self._worker_pessoas, self.config.intervalo_pessoas),
            ("tarefas", self._worker_tarefas, self.config.intervalo_tarefas),
        ]
        for nome, alvo, intervalo in tarefas:
            thread = threading.Thread(target=self._laco, args=(nome, alvo, intervalo),
                                      name=f"alfasync-{nome}", daemon=True)
            thread.start()
            self._threads.append(thread)
        logger.info(f"{len(self._threads)} rotinas ativas. Ctrl+C encerra.")

    def parar(self, *_args) -> None:
        if self._parar.is_set():
            return
        logger.info("Encerrando o gateway…")
        self._parar.set()
        for thread in self._threads:
            thread.join(timeout=10)
        # A fila e o state fecham por ultimo: o ultimo evento coletado precisa
        # estar gravado em disco antes do processo morrer.
        self.state.salvar()
        self.fila.fechar()
        for api in list(self.apis.values()):
            api.fechar()
        logging.shutdown()

    def aguardar(self) -> None:
        while not self._parar.is_set():
            self._parar.wait(1.0)

    def _laco(self, nome: str, alvo: Callable[[], None], intervalo: float) -> None:
        """Cada worker num laco proprio. Excecao aqui nunca derruba as outras rotinas."""
        while not self._parar.is_set():
            inicio = time.time()
            try:
                alvo()
            except Exception as e:
                logger.error(f"[{nome}] erro inesperado: {e}", exc_info=logger.isEnabledFor(logging.DEBUG))
            resto = intervalo - (time.time() - inicio)
            self._parar.wait(max(0.5, resto))

    # ─── equipamentos ─────────────────────────────────────────────────────────

    def _recarregar_dispositivos(self) -> None:
        try:
            do_backend = self.backend.listar_dispositivos()
        except BackendApiError as e:
            logger.warning(f"Nao consegui listar equipamentos no backend ({e}) — usando o config.json.")
            do_backend = []

        lista = mesclar_dispositivos(self.config.devices, do_backend)
        with self._devices_lock:
            self.devices = {d["id"]: d for d in lista}
            for device in lista:
                api = self.apis.get(device["id"])
                if api is None or api.host != device["host"]:
                    if api is not None:
                        api.fechar()
                    self.apis[device["id"]] = ControlIdDevice(
                        host=device["host"], port=device["port"],
                        username=device["username"], password=device["password"],
                        timeout=self.config.timeout_dispositivo,
                        sessao_ttl=self.config.sessao_ttl_s, nome=device["nome"],
                    )
            for device_id in list(self.apis):
                if device_id not in self.devices:
                    self.apis.pop(device_id).fechar()
        self._ultima_carga_devices = time.time()
        descricao = ", ".join(f"{d['nome']} ({d['host']})" for d in lista) or "nenhum"
        logger.info(f"{len(self.devices)} equipamento(s) sob este gateway: {descricao}")

    def _pares(self) -> List[Tuple[Dict[str, Any], ControlIdDevice]]:
        with self._devices_lock:
            return [(d, self.apis[i]) for i, d in self.devices.items() if i in self.apis]

    # ─── log por transicao ────────────────────────────────────────────────────

    def _marcar_leitor(self, device: Dict[str, Any], online: bool, motivo: str = "") -> None:
        with self._log_lock:
            anterior = self._leitor_online.get(device["id"])
            self._leitor_online[device["id"]] = online
        if anterior == online:
            if not online:
                logger.debug(f"{device['nome']} segue fora do ar: {motivo}")
            return
        if online:
            logger.info(f"{device['nome']} ({device['host']}) respondendo de novo.")
        else:
            detalhe = f": {motivo}." if motivo else "."
            logger.warning(
                f"{device['nome']} ({device['host']}) parou de responder na rede local"
                f"{detalhe} Confira energia, cabo e o IP do leitor."
            )

    def _marcar_backend(self, ok: bool, motivo: str = "") -> None:
        with self._log_lock:
            if self._backend_ok == ok:
                if not ok:
                    logger.debug(f"Backend segue inacessivel: {motivo}")
                return
            anterior_caiu_em = self._backend_caiu_em
            self._backend_ok = ok
            self._backend_caiu_em = 0.0 if ok else time.time()

        if ok:
            if anterior_caiu_em:
                fora = int(time.time() - anterior_caiu_em)
                logger.info(f"Backend reconectado apos {fora}s. Drenando a fila.")
        else:
            logger.warning(f"Backend inacessivel: {motivo} — eventos vao para a fila local.")

    # ─── workers ──────────────────────────────────────────────────────────────

    def _worker_heartbeat(self) -> None:
        # A lista de equipamentos pode mudar no backend (leitor novo na escola);
        # recarrega de tempos em tempos, junto do heartbeat.
        if time.time() - self._ultima_carga_devices > 300:
            self._recarregar_dispositivos()

        for device, api in self._pares():
            alcancavel, ping_ms = api.ping(timeout=min(3.0, self.config.timeout_dispositivo))
            corpo = {
                "deviceId": str(device["id"]),
                "deviceOnline": alcancavel,
                "agentVersion": VERSAO,
                "queueSize": self.fila.total(),
            }
            if ping_ms is not None:
                corpo["pingResponseTimeMs"] = ping_ms
            try:
                self.backend.enviar_heartbeat(corpo)
                self._marcar_backend(True)
            except BackendApiError as e:
                self._marcar_backend(False, str(e))
            self._marcar_leitor(device, alcancavel)

    def _worker_eventos(self) -> None:
        for device, api in self._pares():
            if self._parar.is_set():
                return
            device_id = device["id"]
            cursor = self.state.last_event_id(device_id)
            try:
                eventos = api.listar_eventos(desde_id=cursor, limite=self.config.eventos_lote)
            except DeviceApiError as e:
                self._marcar_leitor(device, False, str(e))
                continue
            self._marcar_leitor(device, True)

            eventos = sorted((e for e in eventos if e.get("id") is not None),
                             key=lambda e: int(e.get("id", 0)))

            # ── Contador do leitor reiniciado (bug classico) ──────────────────
            # Dois sintomas, porque o firmware pode dar qualquer um dos dois:
            #
            # (a) lote VAZIO com cursor > 0. E o caso comum: o filtro e
            #     `id > cursor-1`, entao depois de um format (ids voltam ao 1)
            #     nenhum log novo satisfaz o filtro e o leitor responde vazio
            #     para sempre. Sem tratar isso, o gateway fica mudo em silencio:
            #     nenhum erro no log, e a lista de presenca simplesmente vazia.
            # (b) lote com id MENOR que o cursor, quando o firmware ignora o
            #     filtro e devolve o historico do inicio.
            if not eventos:
                if cursor > 0:
                    vazios = self._polls_vazios.get(device_id, 0) + 1
                    self._polls_vazios[device_id] = vazios
                    if vazios >= POLLS_VAZIOS_ANTES_DE_REINICIAR:
                        logger.warning(
                            f"{device['nome']}: {vazios} leituras seguidas sem o log de "
                            f"referencia (cursor {cursor}) — historico do leitor foi "
                            "limpo. Reimportando do inicio."
                        )
                        self.state.set_last_event_id(device_id, 0)
                        self.state.salvar()
                        self._polls_vazios[device_id] = 0
                continue
            self._polls_vazios[device_id] = 0

            if self.state.detectar_reinicio_contador(device_id, int(eventos[0]["id"])):
                cursor = 0

            novos = 0
            for evento in eventos:
                if self._parar.is_set():
                    break
                event_id = int(evento.get("id", 0))
                if event_id <= cursor:
                    continue    # sentinela / ja processado

                clock.checar_relogio(device["nome"], evento.get("time") or 0)
                payload = montar_evento(device, evento, self.config.client_id)
                ext_id = payload["externalEventId"]

                # Entra na fila ANTES de tentar o envio: se o processo morrer entre
                # o envio e a gravacao do cursor, a passagem nao se perde. O par
                # (device_id, ext_id) e unico, entao isso nunca duplica.
                self.fila.enfileirar(str(device_id), ext_id, payload)
                try:
                    self.backend.enviar_evento(payload)
                    self.fila.remover_por_ext(str(device_id), ext_id)
                    self._marcar_backend(True)
                    novos += 1
                except BackendApiError as e:
                    if e.definitivo:
                        # Recusa definitiva nao e queda: o backend esta de pe e
                        # disse nao. O evento fica na fila e sai de la no
                        # proximo ciclo de reenvio (drenar_fila), com o motivo.
                        logger.warning(f"{device['nome']}: backend recusou o acesso: {e}")
                    else:
                        self._marcar_backend(False, str(e))
                cursor = event_id
                self.state.set_last_event_id(device_id, event_id)

            self.state.salvar()
            if novos:
                logger.info(f"{device['nome']}: {novos} passagem(ns) registrada(s).")

    def _worker_fila(self) -> None:
        enviados, descartados = drenar_fila(
            self.fila, self.backend.enviar_evento, limite=self.config.fila_lote,
        )
        if enviados or descartados:
            logger.info(f"Fila: {enviados} reenviado(s), {descartados} descartado(s), "
                        f"{self.fila.total()} restante(s).")
        self.fila.prune_old(self.config.fila_retencao_horas)

    def _worker_pessoas(self) -> None:
        """Delta de pessoas e faces. Paginado, com cursor em state.json."""
        cursor = self.state.users_updated_after
        pagina = 0
        maior_visto = cursor
        total = 0
        while not self._parar.is_set():
            try:
                pessoas, tem_mais = self.backend.listar_pessoas(atualizadas_apos=cursor,
                                                               pagina=pagina)
            except BackendApiError as e:
                self._marcar_backend(False, str(e))
                logger.debug(f"Sincronizacao de pessoas adiada: {e}")
                return
            if not pessoas:
                break
            for bruta in pessoas:
                pessoa = normalizar_pessoa(bruta)
                if pessoa["id"] is None:
                    continue
                self._aplicar_pessoa(pessoa)
                total += 1
                marca = para_epoch_millis(pessoa["atualizado_em"])
                if marca and (maior_visto is None or marca > maior_visto):
                    maior_visto = marca
            if not tem_mais:
                break
            pagina += 1

        if total:
            logger.info(f"Sincronizacao: {total} pessoa(s) aplicada(s) nos leitores.")
        if maior_visto and maior_visto != cursor:
            # Cursor so avanca com o updatedAt que o BACKEND carimbou. Usar o
            # relogio do gateway aqui perderia alteracoes quando os dois relogios
            # divergem (e eles divergem).
            self.state.users_updated_after = maior_visto
        self.state.salvar()

    def _aplicar_pessoa(self, pessoa: Dict[str, Any],
                        apenas_device: Optional[str] = None) -> Tuple[int, List[str]]:
        """Grava (ou remove) a pessoa nos leitores. Devolve (sucessos, erros)."""
        sucessos = 0
        erros: List[str] = []
        for device, api in self._pares():
            if apenas_device and str(device["id"]) != str(apenas_device):
                continue
            try:
                if not pessoa["ativo"]:
                    api.remover_usuario(int(pessoa["id"]))
                    self.state.esquecer_foto(device["id"], pessoa["id"])
                    sucessos += 1
                    continue

                api.criar_usuario(int(pessoa["id"]), pessoa["nome"], pessoa["matricula"])
                api.vincular_grupo(int(pessoa["id"]))

                foto = pessoa.get("foto")
                if foto:
                    assinatura = hash_foto(foto)
                    # Sem esta comparacao, a mesma face seria reenviada a cada
                    # ciclo de 30s para cada leitor — o equipamento fica ocupado
                    # processando imagem justamente na hora da entrada.
                    if self.state.foto_ja_enviada(device["id"], pessoa["id"], assinatura):
                        sucessos += 1
                        continue
                    api.enviar_foto(int(pessoa["id"]), foto)
                    self.state.registrar_foto(device["id"], pessoa["id"], assinatura)
                    logger.info(f"{device['nome']}: face de {pessoa['nome'] or pessoa['id']} atualizada.")
                sucessos += 1
            except DeviceApiError as e:
                erros.append(f"{device['nome']}: {e}")
                logger.warning(f"{device['nome']}: falha ao sincronizar pessoa {pessoa['id']}: {e}")
        return sucessos, erros

    def _worker_tarefas(self) -> None:
        try:
            tarefas = self.backend.tarefas_pendentes()
        except BackendApiError as e:
            logger.debug(f"Falha ao buscar tarefas: {e}")
            return

        for tarefa in tarefas:
            if self._parar.is_set():
                return
            task_id = tarefa.get("id")
            tipo = tipo_da_tarefa(tarefa)
            dados = payload_da_tarefa(tarefa)
            try:
                sucesso, erro = self._executar_tarefa(tipo, dados)
            except Exception as e:
                sucesso, erro = False, str(e)
                logger.error(f"Tarefa {task_id} ({tipo}) quebrou: {e}")
            self.backend.responder_tarefa(task_id, sucesso, erro)
            nivel = logger.info if sucesso else logger.warning
            nivel(f"Tarefa {task_id} ({tipo}): {'ok' if sucesso else erro}")

    def _executar_tarefa(self, tipo: str, dados: Dict[str, Any]) -> Tuple[bool, str]:
        device_id = dados.get("deviceId") or dados.get("dispositivoId") or dados.get("device_id")

        if tipo in ("sincronizar_pessoa", "sync_person", "sincronizarpessoa"):
            pessoa = normalizar_pessoa(dados.get("pessoa") or dados.get("person") or dados)
            if pessoa["id"] is None:
                return False, "tarefa sem id de pessoa"
            sucessos, erros = self._aplicar_pessoa(pessoa, apenas_device=device_id)
            self.state.salvar()
            if sucessos == 0:
                return False, "; ".join(erros) or "nenhum leitor disponivel"
            return True, "; ".join(erros)

        if tipo in ("remover_pessoa", "remove_person", "removerpessoa"):
            pessoa_id = (dados.get("pessoaId") or dados.get("personId")
                         or dados.get("id") or dados.get("pessoa_id"))
            if pessoa_id is None:
                return False, "tarefa sem id de pessoa"
            erros = []
            removidos = 0
            for device, api in self._pares():
                if device_id and str(device["id"]) != str(device_id):
                    continue
                try:
                    api.remover_usuario(int(pessoa_id))
                    self.state.esquecer_foto(device["id"], pessoa_id)
                    removidos += 1
                except DeviceApiError as e:
                    erros.append(f"{device['nome']}: {e}")
            self.state.salvar()
            return (removidos > 0), "; ".join(erros)

        if tipo in ("abrir_acesso", "open_door", "abriracesso", "abrir_porta"):
            # Comando do backend, nao decisao do gateway: alguem na secretaria
            # clicou em "liberar". O gateway so pulsa o rele.
            alvos = [(d, a) for d, a in self._pares()
                     if not device_id or str(d["id"]) == str(device_id)]
            if not alvos:
                return False, f"equipamento {device_id} nao esta neste gateway"
            erros = []
            abertos = 0
            for device, api in alvos:
                porta = int(dados.get("doorId") or dados.get("porta") or device.get("door_id", 1))
                try:
                    api.abrir_porta(porta)
                    abertos += 1
                    logger.info(f"{device['nome']}: acesso aberto por comando do backend.")
                except DeviceApiError as e:
                    erros.append(f"{device['nome']}: {e}")
            return (abertos > 0), "; ".join(erros)

        return False, f"comando desconhecido: {tipo!r}"


# ─── log e main ───────────────────────────────────────────────────────────────

def configurar_log(config: Config) -> None:
    """Arquivo rotativo E tela: quem instala olha a tela, quem da suporte le o arquivo."""
    raiz = logging.getLogger()
    raiz.setLevel(getattr(logging, config.log_nivel, logging.INFO))
    for handler in list(raiz.handlers):
        raiz.removeHandler(handler)

    formato = logging.Formatter("%(asctime)s %(levelname)-7s %(name)s: %(message)s",
                                datefmt="%Y-%m-%d %H:%M:%S")

    tela = logging.StreamHandler(sys.stdout)
    tela.setFormatter(formato)
    raiz.addHandler(tela)

    try:
        os.makedirs(os.path.dirname(os.path.abspath(config.log_arquivo)), exist_ok=True)
        arquivo = logging.handlers.RotatingFileHandler(
            config.log_arquivo, maxBytes=config.log_max_bytes,
            backupCount=config.log_backups, encoding="utf-8",
        )
        arquivo.setFormatter(formato)
        raiz.addHandler(arquivo)
    except Exception as e:
        # Sem permissao de escrita o gateway ainda roda; so perde o historico.
        print(f"AVISO: log em arquivo desativado ({e})", file=sys.stderr)

    logging.getLogger("urllib3").setLevel(logging.WARNING)


def main(argv: Optional[List[str]] = None) -> int:
    argv = argv if argv is not None else sys.argv[1:]
    caminho = argv[0] if argv else None
    try:
        config = carregar(caminho)
    except ConfigError as e:
        print(f"ERRO DE CONFIGURACAO\n{e}", file=sys.stderr)
        return 2

    configurar_log(config)
    gateway = Gateway(config)

    def encerrar(signum, _frame):
        logger.info(f"Sinal {signum} recebido.")
        gateway.parar()

    signal.signal(signal.SIGINT, encerrar)
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, encerrar)

    try:
        gateway.iniciar()
        gateway.aguardar()
    except KeyboardInterrupt:
        pass
    finally:
        gateway.parar()
    return 0


if __name__ == "__main__":
    sys.exit(main())
