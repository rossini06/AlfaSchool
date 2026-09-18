"""Leitor Control iD: cache de sessao, timeouts e montagem do corpo."""
import json

import pytest
import requests

from device_api import ControlIdDevice, DeviceApiError


class RespostaFake:
    def __init__(self, status=200, corpo=None):
        self.status_code = status
        self._corpo = corpo if corpo is not None else {}
        self.text = json.dumps(self._corpo)
        self.content = self.text.encode()

    def json(self):
        return self._corpo


class HttpFake:
    def __init__(self):
        self.chamadas = []           # (url, corpo_decodificado, timeout)
        self.respostas = {}          # sufixo do endpoint -> lista de respostas
        self.logins = 0

    def post(self, url, data=None, headers=None, timeout=None):
        corpo = data
        if isinstance(data, (bytes, bytearray)) and headers and \
                headers.get("Content-Type") == "application/json":
            corpo = data.decode()
        if isinstance(corpo, str):
            try:
                corpo = json.loads(corpo)
            except Exception:
                pass
        self.chamadas.append((url, corpo, timeout))

        if "/login.fcgi" in url:
            self.logins += 1
            return RespostaFake(200, {"session": f"sessao-{self.logins}"})
        for sufixo, respostas in self.respostas.items():
            if sufixo in url:
                return respostas.pop(0) if len(respostas) > 1 else respostas[0]
        return RespostaFake(200, {})

    def close(self):
        pass


@pytest.fixture
def leitor():
    device = ControlIdDevice("192.168.0.10", 80, "admin", "senha", timeout=5,
                             sessao_ttl=300, nome="PORTARIA")
    device._http = HttpFake()
    return device


def test_sessao_e_reaproveitada_entre_chamadas(leitor):
    """Login a cada operacao derruba o desempenho do leitor."""
    leitor.listar_eventos(0, 10)
    leitor.listar_eventos(0, 10)
    leitor.listar_eventos(0, 10)
    assert leitor._http.logins == 1


def test_sessao_expira_pelo_ttl(leitor):
    leitor.sessao_ttl = 40           # TTL curto: renova com folga de 15s
    leitor.listar_eventos(0, 10)
    leitor._sessao_expira_em = 0     # simula o tempo passando
    leitor.listar_eventos(0, 10)
    assert leitor._http.logins == 2


def test_401_do_leitor_invalida_a_sessao_e_refaz_login(leitor):
    leitor._http.respostas["/load_objects.fcgi"] = [
        RespostaFake(401, {}), RespostaFake(200, {"access_logs": [{"id": 1}]}),
    ]
    eventos = leitor.listar_eventos(0, 10)
    assert eventos == [{"id": 1}]
    assert leitor._http.logins == 2


def test_sessao_vai_na_query_string(leitor):
    leitor.listar_eventos(0, 10)
    url = leitor._http.chamadas[-1][0]
    assert "session=sessao-1" in url


def test_corpo_de_eventos_usa_filtro_por_id_inclusivo(leitor):
    leitor.listar_eventos(desde_id=100, limite=50)
    _url, corpo, _timeout = leitor._http.chamadas[-1]
    assert corpo["object"] == "access_logs"
    # `> desde_id - 1` = inclusivo: o ultimo log processado volta como sentinela.
    assert corpo["where"]["access_logs"]["id"] == {">": 99}
    assert corpo["limit"] == 50


def test_nome_com_aspas_nao_quebra_o_json(leitor):
    """Motivo de o corpo ir por json.dumps, nunca por concatenacao."""
    leitor.criar_usuario(5, 'Ana D\'Avila "A"', "2026-1")
    _url, corpo, _timeout = leitor._http.chamadas[-1]
    assert corpo["values"][0]["name"] == 'Ana D\'Avila "A"'


def test_abrir_porta_usa_parameters_door(leitor):
    """`id=N` responde 200 e NAO pulsa o rele — armadilha do firmware."""
    leitor.abrir_porta(2)
    _url, corpo, _timeout = leitor._http.chamadas[-1]
    assert corpo["actions"][0] == {"action": "door", "parameters": "door=2"}


def test_toda_chamada_tem_timeout(leitor):
    leitor.listar_eventos(0, 10)
    leitor.abrir_porta(1)
    assert all(timeout is not None for _url, _corpo, timeout in leitor._http.chamadas)


def test_leitor_mudo_vira_erro_claro(leitor):
    class HttpMudo(HttpFake):
        def post(self, url, **kwargs):
            raise requests.exceptions.Timeout("tempo esgotado")

    leitor._http = HttpMudo()
    with pytest.raises(DeviceApiError) as erro:
        leitor.listar_eventos(0, 10)
    assert "PORTARIA" in str(erro.value)


def test_remover_usuario_monta_where(leitor):
    leitor.remover_usuario(9)
    _url, corpo, _timeout = leitor._http.chamadas[-1]
    assert corpo == {"object": "users", "where": {"users": {"id": 9}}}


def test_foto_invalida_nao_vai_para_o_leitor(leitor):
    with pytest.raises(DeviceApiError):
        leitor.enviar_foto(5, "")
