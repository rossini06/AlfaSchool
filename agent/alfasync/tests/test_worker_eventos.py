"""Coleta de eventos ponta a ponta, com leitor e backend falsos.

Cobre a fiacao que os testes de unidade nao pegam: cursor, fila, idempotencia e
a reacao ao contador do leitor reiniciar.
"""
import calendar

import pytest

from agent import Gateway
from backend_api import BackendApiError
from config import Config
from device_api import DeviceApiError


def epoch(dia, hora, minuto):
    return calendar.timegm((2026, 9, dia, hora, minuto, 0, 0, 0))


class LeitorFake:
    """So o que o worker usa do ControlIdDevice."""

    def __init__(self, eventos=None):
        self.eventos = eventos or []
        self.host = "192.168.0.10"
        self.nome = "PORTARIA"
        self.aberturas = []
        self.removidos = []

    def listar_eventos(self, desde_id=0, limite=200):
        return [e for e in self.eventos if int(e["id"]) >= int(desde_id)]

    def ping(self, timeout=3.0):
        return True, 5

    def abrir_porta(self, door_id=1):
        self.aberturas.append(door_id)
        return {"success": True}

    def remover_usuario(self, user_id):
        self.removidos.append(user_id)
        return {}

    def fechar(self):
        pass


DEVICE = {"id": "7", "nome": "PORTARIA", "host": "192.168.0.10", "port": 80,
          "username": "admin", "password": "x", "modelo": "controlid_idface",
          "door_id": 1, "ativo": True}


@pytest.fixture
def gateway(tmp_path):
    cfg = Config({
        "backend": {"base_url": "https://escola.exemplo", "client_id": "uuid-1",
                    "username": "SYNC", "password": "senha"},
        "devices": [],
        "dados_dir": str(tmp_path),
    })
    g = Gateway(cfg)
    yield g
    g.fila.fechar()


def instalar_leitor(gateway, leitor):
    gateway.devices = {"7": dict(DEVICE)}
    gateway.apis = {"7": leitor}


def test_evento_novo_vai_ao_backend_e_avanca_o_cursor(gateway):
    leitor = LeitorFake([{"id": 10, "user_id": 500, "time": epoch(18, 7, 30), "event": 4}])
    instalar_leitor(gateway, leitor)
    enviados = []
    gateway.backend.enviar_evento = lambda p: enviados.append(p)

    gateway._worker_eventos()

    assert len(enviados) == 1
    assert enviados[0]["personId"] == "500"
    assert enviados[0]["timestamp"] == "2026-09-18T07:30:00-03:00"
    assert gateway.state.last_event_id("7") == 10
    assert gateway.fila.total() == 0        # entregue: nao fica na fila


def test_sentinela_nao_e_reenviada(gateway):
    """`desde_id` e inclusivo: o ultimo log volta no lote e deve ser ignorado."""
    leitor = LeitorFake([{"id": 10, "user_id": 500, "time": epoch(18, 7, 30), "event": 4}])
    instalar_leitor(gateway, leitor)
    enviados = []
    gateway.backend.enviar_evento = lambda p: enviados.append(p)

    gateway._worker_eventos()
    gateway._worker_eventos()
    assert len(enviados) == 1


def test_backend_fora_guarda_na_fila_sem_perder_passagem(gateway):
    leitor = LeitorFake([{"id": 10, "user_id": 500, "time": epoch(18, 7, 30), "event": 4}])
    instalar_leitor(gateway, leitor)

    def cai(_payload):
        raise BackendApiError("sem internet", transitorio=True)

    gateway.backend.enviar_evento = cai
    gateway._worker_eventos()

    assert gateway.fila.total() == 1
    # E o reenvio entrega quando a internet volta, uma vez so.
    entregues = []
    gateway.backend.enviar_evento = lambda p: entregues.append(p)
    gateway._worker_fila()
    assert len(entregues) == 1
    assert gateway.fila.total() == 0


def test_contador_reiniciado_reimporta_em_vez_de_travar(gateway):
    """Leitor formatado: ids voltam ao 1 e o gateway precisa voltar junto.

    Sintoma real: o filtro e `id > cursor-1`, entao com o cursor em 48312 e os
    ids de volta ao 1 o leitor responde SEMPRE VAZIO. Sem tratar, o gateway fica
    mudo em silencio — nenhum erro no log e nenhuma presenca registrada.
    """
    leitor = LeitorFake([{"id": 1, "user_id": 500, "time": epoch(18, 8, 0), "event": 4},
                         {"id": 2, "user_id": 501, "time": epoch(18, 8, 1), "event": 4}])
    instalar_leitor(gateway, leitor)
    gateway.state.set_last_event_id("7", 48312)
    enviados = []
    gateway.backend.enviar_evento = lambda p: enviados.append(p)

    # O leitor so devolve o que casa com o filtro: com o cursor la na frente, nada.
    for _ in range(2):
        gateway._worker_eventos()
    assert enviados == []                          # tolerancia a lote vazio pontual
    assert gateway.state.last_event_id("7") == 48312

    gateway._worker_eventos()                      # terceiro vazio: zera o cursor
    assert gateway.state.last_event_id("7") == 0

    gateway._worker_eventos()                      # agora reimporta
    assert len(enviados) == 2
    assert gateway.state.last_event_id("7") == 2


def test_lote_vazio_pontual_nao_reimporta(gateway):
    """Um leitor ocupado pode devolver vazio uma vez; isso nao e format."""
    leitor = LeitorFake([{"id": 10, "user_id": 1, "time": epoch(18, 8, 0), "event": 4}])
    instalar_leitor(gateway, leitor)
    gateway.backend.enviar_evento = lambda p: None
    gateway._worker_eventos()
    assert gateway.state.last_event_id("7") == 10

    leitor.eventos = []                            # soluco do firmware
    gateway._worker_eventos()
    gateway._worker_eventos()
    assert gateway.state.last_event_id("7") == 10  # cursor preservado


def test_firmware_que_ignora_o_filtro_tambem_e_detectado(gateway):
    """Outro sintoma do mesmo bug: o lote volta com ids menores que o cursor."""
    class LeitorSemFiltro(LeitorFake):
        def listar_eventos(self, desde_id=0, limite=200):
            return list(self.eventos)

    leitor = LeitorSemFiltro([{"id": 1, "user_id": 500, "time": epoch(18, 8, 0), "event": 4}])
    instalar_leitor(gateway, leitor)
    gateway.state.set_last_event_id("7", 48312)
    enviados = []
    gateway.backend.enviar_evento = lambda p: enviados.append(p)

    gateway._worker_eventos()
    assert len(enviados) == 1
    assert gateway.state.last_event_id("7") == 1


def test_leitor_mudo_nao_derruba_os_outros(gateway):
    class LeitorMudo(LeitorFake):
        def listar_eventos(self, desde_id=0, limite=200):
            raise DeviceApiError("timeout")

    gateway.devices = {"7": dict(DEVICE), "8": dict(DEVICE, id="8", nome="QUADRA")}
    bom = LeitorFake([{"id": 3, "user_id": 1, "time": epoch(18, 9, 0), "event": 4}])
    gateway.apis = {"7": LeitorMudo(), "8": bom}
    enviados = []
    gateway.backend.enviar_evento = lambda p: enviados.append(p)

    gateway._worker_eventos()
    assert len(enviados) == 1


def test_tarefa_abrir_acesso_pulsa_o_rele(gateway):
    leitor = LeitorFake()
    instalar_leitor(gateway, leitor)
    ok, erro = gateway._executar_tarefa("abrir_acesso", {"deviceId": "7"})
    assert ok is True and erro == ""
    assert leitor.aberturas == [1]


def test_tarefa_para_equipamento_de_outro_gateway_falha_com_motivo(gateway):
    instalar_leitor(gateway, LeitorFake())
    ok, erro = gateway._executar_tarefa("abrir_acesso", {"deviceId": "99"})
    assert ok is False
    assert "99" in erro


def test_tarefa_remover_pessoa(gateway):
    leitor = LeitorFake()
    instalar_leitor(gateway, leitor)
    gateway.state.registrar_foto("7", 500, "hash")
    ok, _erro = gateway._executar_tarefa("remover_pessoa", {"pessoaId": 500})
    assert ok is True
    assert leitor.removidos == [500]
    # Esquecer o hash garante o reenvio da face num recadastro.
    assert gateway.state.hash_da_foto("7", 500) is None


def test_comando_desconhecido_volta_como_erro(gateway):
    instalar_leitor(gateway, LeitorFake())
    ok, erro = gateway._executar_tarefa("formatar_leitor", {})
    assert ok is False
    assert "desconhecido" in erro
