"""Fila: idempotencia, backoff e retencao."""
import time

import pytest

from event_queue import EventQueue, proximo_intervalo


@pytest.fixture
def fila():
    f = EventQueue(":memory:", backoff_base=10, backoff_teto=100)
    yield f
    f.fechar()


def test_mesmo_device_e_ext_id_gera_uma_linha(fila):
    """Idempotencia no proprio gateway: o evento pode ser reprocessado."""
    assert fila.enfileirar("1", "1:42:1700000000", {"a": 1}) is True
    assert fila.enfileirar("1", "1:42:1700000000", {"a": 1}) is False
    assert fila.total() == 1


def test_ext_id_igual_em_devices_diferentes_sao_eventos_distintos(fila):
    fila.enfileirar("1", "42", {"a": 1})
    fila.enfileirar("2", "42", {"a": 2})
    assert fila.total() == 2


def test_ordem_preservada(fila):
    for i in range(3):
        fila.enfileirar("1", f"e{i}", {"i": i})
    ids = [linha["ext_id"] for linha in fila.proximos()]
    assert ids == ["e0", "e1", "e2"]


def test_backoff_cresce_a_cada_tentativa():
    valores = [proximo_intervalo(n, base=10, teto=1000) for n in range(1, 6)]
    assert valores == [10, 20, 40, 80, 160]
    assert all(b > a for a, b in zip(valores, valores[1:]))
    # Sem tentativa nenhuma, o primeiro reenvio sai na hora.
    assert proximo_intervalo(0, base=10) == 0
    # E o teto segura o crescimento.
    assert proximo_intervalo(50, base=10, teto=900) == 900


def test_evento_em_backoff_nao_reaparece_antes_da_hora(fila):
    fila.enfileirar("1", "e1", {"a": 1})
    agora = time.time()
    linha = fila.proximos(agora=agora)[0]

    fila.marcar_falha(linha["id"], "backend fora", agora=agora)
    # base=10s: logo apos a falha ainda nao esta pronto.
    assert fila.proximos(agora=agora + 5) == []
    assert len(fila.proximos(agora=agora + 11)) == 1

    # Segunda falha dobra a espera.
    fila.marcar_falha(linha["id"], "backend fora", agora=agora + 11)
    assert fila.proximos(agora=agora + 25) == []
    assert len(fila.proximos(agora=agora + 35)) == 1


def test_marcar_falha_conta_tentativa_e_mantem_na_fila(fila):
    fila.enfileirar("1", "e1", {"a": 1})
    linha = fila.proximos()[0]
    fila.marcar_falha(linha["id"], "timeout")
    assert fila.total() == 1
    assert fila.proximos(agora=time.time() + 999)[0]["attempts"] == 1


def test_prune_old_remove_o_que_passou_do_prazo(fila):
    fila.enfileirar("1", "antigo", {"a": 1})
    fila._conn.execute("UPDATE pending_events SET created_at = ?",
                       (time.time() - 100 * 3600,))
    fila.enfileirar("1", "novo", {"a": 2})
    assert fila.prune_old(72) == 1
    assert [l["ext_id"] for l in fila.proximos()] == ["novo"]


def test_remover_por_ext(fila):
    fila.enfileirar("1", "e1", {"a": 1})
    fila.remover_por_ext("1", "e1")
    assert fila.total() == 0
