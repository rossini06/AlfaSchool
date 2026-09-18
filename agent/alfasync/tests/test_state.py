"""Estado: reinicio do contador do leitor e hash de fotos."""
import os

import pytest

from state import State, hash_foto


@pytest.fixture
def estado(tmp_path):
    return State(str(tmp_path / "state.json"))


def test_detecta_reinicio_do_contador_do_leitor(estado):
    """Leitor formatado: access_logs.id volta a contar do 1.

    Sem detectar, o gateway compara "id > cursor" para sempre e nunca mais envia
    passagem nenhuma — em silencio, que e o pior do bug.
    """
    estado.set_last_event_id("1", 48312)
    assert estado.detectar_reinicio_contador("1", 3) is True
    assert estado.last_event_id("1") == 0


def test_evento_maior_que_o_cursor_nao_e_reinicio(estado):
    estado.set_last_event_id("1", 100)
    assert estado.detectar_reinicio_contador("1", 101) is False
    assert estado.last_event_id("1") == 100


def test_evento_igual_ao_cursor_e_a_sentinela_nao_e_reinicio(estado):
    """O `desde_id` e inclusivo: o ultimo log processado volta no lote."""
    estado.set_last_event_id("1", 100)
    assert estado.detectar_reinicio_contador("1", 100) is False
    assert estado.last_event_id("1") == 100


def test_primeira_execucao_nao_e_reinicio(estado):
    assert estado.detectar_reinicio_contador("novo", 5) is False


def test_reinicio_e_por_equipamento(estado):
    estado.set_last_event_id("1", 500)
    estado.set_last_event_id("2", 500)
    estado.detectar_reinicio_contador("1", 2)
    assert estado.last_event_id("1") == 0
    assert estado.last_event_id("2") == 500


def test_foto_identica_nao_e_reenviada(estado):
    assinatura = hash_foto("Zm90by1iYXNlNjQ=")
    assert estado.foto_ja_enviada("1", 7, assinatura) is False
    estado.registrar_foto("1", 7, assinatura)
    assert estado.foto_ja_enviada("1", 7, assinatura) is True
    # Foto trocada no cadastro: hash muda e o envio volta a acontecer.
    assert estado.foto_ja_enviada("1", 7, hash_foto("outra")) is False
    # Pessoa removida: esquecer garante o reenvio num recadastro.
    estado.esquecer_foto("1", 7)
    assert estado.foto_ja_enviada("1", 7, assinatura) is False


def test_persiste_entre_partidas(tmp_path):
    caminho = str(tmp_path / "state.json")
    primeiro = State(caminho)
    primeiro.set_last_event_id("1", 77)
    primeiro.registrar_foto("1", 9, "abc")
    primeiro.users_updated_after = 1700000000000
    primeiro.salvar()

    segundo = State(caminho)
    assert segundo.last_event_id("1") == 77
    assert segundo.hash_da_foto("1", 9) == "abc"
    assert segundo.users_updated_after == 1700000000000


def test_state_corrompido_nao_derruba_o_gateway(tmp_path):
    caminho = tmp_path / "state.json"
    caminho.write_text("{ isto nao e json", encoding="utf-8")
    estado = State(str(caminho))
    assert estado.last_event_id("1") == 0


def test_gravacao_nao_deixa_arquivo_temporario(tmp_path):
    caminho = str(tmp_path / "state.json")
    estado = State(caminho)
    estado.set_last_event_id("1", 1)
    estado.salvar()
    assert os.path.exists(caminho)
    assert not os.path.exists(caminho + ".tmp")
