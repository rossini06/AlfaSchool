"""Cliente do backend: envelope, reautenticacao em 401 e tolerancia a 404."""
import json
import logging

import pytest
import requests

from backend_api import BackendApi, BackendApiError, desembrulhar


class RespostaFake:
    def __init__(self, status=200, corpo=None, json_content=True):
        self.status_code = status
        self._corpo = corpo
        self.headers = {"Content-Type": "application/json" if json_content else "text/plain"}
        self.text = json.dumps(corpo) if corpo is not None else ""
        self.content = self.text.encode()

    def json(self):
        if self._corpo is None:
            raise ValueError("sem corpo")
        return self._corpo


def envelope(data, status=200, message="ok"):
    return {"timestamp": "2026-09-18T10:00:00-03:00", "status": status,
            "message": message, "data": data}


class SessaoFake:
    """Devolve respostas roteirizadas e guarda o que foi chamado."""

    def __init__(self, roteiro):
        self.roteiro = roteiro      # {(metodo, rota_sufixo): [respostas]}
        self.chamadas = []

    def _pegar(self, metodo, url):
        for (m, sufixo), respostas in self.roteiro.items():
            if m == metodo and url.endswith(sufixo):
                return respostas.pop(0) if len(respostas) > 1 else respostas[0]
        raise AssertionError(f"chamada inesperada: {metodo} {url}")

    def post(self, url, **kwargs):
        self.chamadas.append(("post", url))
        return self._pegar("post", url)

    def request(self, metodo, url, **kwargs):
        self.chamadas.append((metodo, url))
        return self._pegar(metodo, url)


def montar(roteiro):
    api = BackendApi("https://escola.exemplo", "uuid-1", "SYNC", "senha")
    api._sessao = SessaoFake(roteiro)
    return api


def test_desembrulhar_pega_o_data():
    assert desembrulhar(envelope([1, 2])) == [1, 2]


def test_desembrulhar_aceita_resposta_crua():
    """Backend mais velho, sem envelope, nao pode quebrar o gateway."""
    assert desembrulhar([1, 2]) == [1, 2]
    assert desembrulhar({"id": 1}) == {"id": 1}


def test_login_extrai_o_token():
    api = montar({("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))]})
    assert api.autenticar() == "T1"
    assert api.token == "T1"


def test_login_aceita_accessToken():
    api = montar({("post", "/login"): [RespostaFake(200, envelope({"accessToken": "T2"}))]})
    assert api.autenticar() == "T2"


def test_401_reautentica_e_repete_uma_vez():
    api = montar({
        ("post", "/login"): [RespostaFake(200, envelope({"token": "T1"})),
                             RespostaFake(200, envelope({"token": "T2"}))],
        ("get", "/devices"): [RespostaFake(401, envelope(None, 401, "expirado")),
                              RespostaFake(200, envelope([{"id": 1, "ip": "10.0.0.1"}]))],
    })
    devices = api.listar_dispositivos()
    assert devices == [{"id": 1, "ip": "10.0.0.1"}]
    assert api.token == "T2"


def test_404_em_rota_de_leitura_nao_trava_o_ciclo(caplog):
    """Backend mais velho: WARN uma vez, lista vazia, e a vida segue."""
    api = montar({
        ("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))],
        ("get", "/tasks/pending"): [RespostaFake(404, envelope(None, 404, "nao encontrado"))],
    })
    with caplog.at_level(logging.WARNING, logger="backend_api"):
        assert api.tarefas_pendentes() == []
        assert api.tarefas_pendentes() == []
        assert api.tarefas_pendentes() == []
    avisos = [r for r in caplog.records if r.levelno == logging.WARNING]
    # Uma linha por rota, nao uma por ciclo: com polling de 5s, logar sempre
    # encheria o disco da maquina da escola.
    assert len(avisos) == 1
    assert "404" in avisos[0].message


def test_4xx_no_envio_de_evento_e_definitivo():
    api = montar({
        ("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))],
        ("post", "/events"): [RespostaFake(422, envelope(None, 422, "pessoa desconhecida"))],
    })
    with pytest.raises(BackendApiError) as erro:
        api.enviar_evento({"x": 1})
    assert erro.value.definitivo is True
    assert erro.value.status_code == 422
    assert "pessoa desconhecida" in str(erro.value)


def test_5xx_no_envio_de_evento_e_transitorio():
    api = montar({
        ("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))],
        ("post", "/events"): [RespostaFake(503, envelope(None, 503, "indisponivel"))],
    })
    with pytest.raises(BackendApiError) as erro:
        api.enviar_evento({"x": 1})
    assert erro.value.transitorio is True


def test_erro_de_rede_e_transitorio():
    class SessaoQueCai(SessaoFake):
        def request(self, metodo, url, **kwargs):
            raise requests.exceptions.ConnectionError("Connection refused")

    api = montar({("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))]})
    api._sessao = SessaoQueCai({})
    api.token = "T1"
    with pytest.raises(BackendApiError) as erro:
        api.enviar_evento({"x": 1})
    assert erro.value.transitorio is True
    assert erro.value.status_code is None


def test_pagina_do_spring_e_entendida():
    api = montar({
        ("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))],
        ("get", "/users"): [RespostaFake(200, envelope(
            {"content": [{"id": 1}], "totalPages": 2}))],
    })
    pessoas, tem_mais = api.listar_pessoas(atualizadas_apos=1)
    assert pessoas == [{"id": 1}]
    assert tem_mais is True


def test_lista_crua_de_pessoas_tambem_serve():
    api = montar({
        ("post", "/login"): [RespostaFake(200, envelope({"token": "T1"}))],
        ("get", "/users"): [RespostaFake(200, envelope([{"id": 1}]))],
    })
    pessoas, tem_mais = api.listar_pessoas()
    assert pessoas == [{"id": 1}]
    assert tem_mais is False
