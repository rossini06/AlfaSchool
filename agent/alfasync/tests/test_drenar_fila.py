"""A fila nao pode travar: politica de erro do reenvio.

Cenario que motivou estes testes em producao: um unico evento recusado em
definitivo na frente de uma fila ordenada segurava todas as passagens seguintes,
indefinidamente.
"""
import pytest

from agent import drenar_fila
from backend_api import BackendApiError, classificar_status
from event_queue import EventQueue


@pytest.fixture
def fila():
    f = EventQueue(":memory:", backoff_base=0, backoff_teto=0)
    yield f
    f.fechar()


def test_classificacao_de_status():
    assert classificar_status(None) is True       # erro de rede
    assert classificar_status(500) is True
    assert classificar_status(503) is True
    assert classificar_status(429) is True
    assert classificar_status(401) is True
    assert classificar_status(400) is False
    assert classificar_status(403) is False
    assert classificar_status(422) is False


def test_4xx_definitivo_tira_da_fila(fila):
    fila.enfileirar("1", "e1", {"x": 1})

    def enviar(_payload):
        raise BackendApiError("evento invalido", status_code=422, transitorio=False)

    enviados, descartados = drenar_fila(fila, enviar)
    assert (enviados, descartados) == (0, 1)
    assert fila.total() == 0


def test_erro_de_rede_mantem_na_fila(fila):
    fila.enfileirar("1", "e1", {"x": 1})

    def enviar(_payload):
        raise BackendApiError("sem conexao", status_code=None, transitorio=True)

    enviados, descartados = drenar_fila(fila, enviar)
    assert (enviados, descartados) == (0, 0)
    assert fila.total() == 1
    assert fila.proximos()[0]["attempts"] == 1


def test_5xx_mantem_na_fila_e_para_o_ciclo(fila):
    fila.enfileirar("1", "e1", {"x": 1})
    fila.enfileirar("1", "e2", {"x": 2})
    chamadas = []

    def enviar(payload):
        chamadas.append(payload)
        raise BackendApiError("backend caiu", status_code=503, transitorio=True)

    drenar_fila(fila, enviar)
    # Parou no primeiro: com o backend fora, insistir nos demais e desperdicio.
    assert len(chamadas) == 1
    assert fila.total() == 2


def test_recusa_definitiva_nao_bloqueia_os_eventos_seguintes(fila):
    fila.enfileirar("1", "ruim", {"x": "ruim"})
    fila.enfileirar("1", "bom", {"x": "bom"})
    entregues = []

    def enviar(payload):
        if payload["x"] == "ruim":
            raise BackendApiError("payload recusado", status_code=400, transitorio=False)
        entregues.append(payload)

    enviados, descartados = drenar_fila(fila, enviar)
    assert (enviados, descartados) == (1, 1)
    assert entregues == [{"x": "bom"}]
    assert fila.total() == 0


def test_404_tolera_algumas_tentativas_e_depois_descarta(fila):
    fila.enfileirar("1", "e1", {"x": 1})

    def enviar(_payload):
        raise BackendApiError("rota ausente", status_code=404, transitorio=True,
                              rota_ausente=True)

    # Backend mais velho: a rota pode aparecer no proximo deploy — nao descarta ja.
    for _ in range(3):
        enviados, descartados = drenar_fila(fila, enviar, max_tentativas_rota_ausente=5)
        assert (enviados, descartados) == (0, 0)
    assert fila.total() == 1

    # Mas nao insiste para sempre.
    for _ in range(3):
        drenar_fila(fila, enviar, max_tentativas_rota_ausente=5)
    assert fila.total() == 0


def test_sucesso_remove_da_fila(fila):
    fila.enfileirar("1", "e1", {"x": 1})
    drenar_fila(fila, lambda _p: None)
    assert fila.total() == 0
