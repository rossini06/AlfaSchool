"""Traducao leitor → backend: evento, pessoa, equipamentos e tarefas."""
import calendar
from datetime import datetime

from agent import (MAPA_EVENTOS, mesclar_dispositivos, montar_evento, montar_ext_id,
                   normalizar_pessoa, para_epoch_millis, payload_da_tarefa,
                   tipo_da_tarefa)

DEVICE = {"id": "7", "nome": "PORTARIA", "host": "192.168.0.10", "port": 80,
          "username": "admin", "password": "x", "modelo": "controlid_idface",
          "door_id": 1, "ativo": True}


def test_evento_de_face_vira_payload_do_backend():
    epoch = calendar.timegm((2026, 9, 18, 7, 5, 3, 0, 0, 0))
    payload = montar_evento(DEVICE, {"id": 42, "user_id": 1001, "time": epoch, "event": 4},
                            client_id="uuid-1")
    assert payload["deviceId"] == "7"
    assert payload["deviceLogId"] == 42
    assert payload["personId"] == "1001"
    assert payload["eventType"] == "access"
    assert payload["method"] == "face"
    assert payload["timestamp"] == "2026-09-18T07:05:03-03:00"
    assert payload["externalEventId"] == f"7:42:{epoch}"


def test_evento_negado_nao_inventa_metodo():
    """Nos codigos de negativa o metodo do firmware nao e confiavel."""
    payload = montar_evento(DEVICE, {"id": 9, "user_id": 3, "time": 1, "event": 5},
                            client_id="uuid-1")
    assert payload["eventType"] == "denied"
    assert payload["method"] is None


def test_evento_sem_horario_usa_o_relogio_do_gateway():
    payload = montar_evento(DEVICE, {"id": 9, "user_id": 3, "time": 0, "event": 4},
                            client_id="uuid-1")
    assert payload["timestamp"].endswith("-03:00")


def test_ext_id_inclui_o_epoch():
    """Depois de um format do leitor os ids reiniciam; o epoch evita colisao."""
    assert montar_ext_id("7", 1, 1700000000) != montar_ext_id("7", 1, 1800000000)


def test_mapa_de_eventos_cobre_os_codigos_usados():
    assert set(MAPA_EVENTOS) >= {1, 2, 3, 4, 5, 6, 7}


def test_pessoa_aceita_grafias_do_backend():
    pt = normalizar_pessoa({"id": 5, "nome": "Ana", "matricula": "2026-1",
                            "fotoBase64": "abc", "ativo": True})
    en = normalizar_pessoa({"id": 5, "name": "Ana", "registration": "2026-1",
                            "photoBase64": "abc", "active": True})
    assert pt == en
    assert pt["id"] == 5 and pt["foto"] == "abc"


def test_pessoa_inativa_e_reconhecida():
    assert normalizar_pessoa({"id": 1, "ativo": False})["ativo"] is False


def test_epoch_millis_aceita_segundos_millis_e_iso():
    assert para_epoch_millis(1700000000) == 1700000000000
    assert para_epoch_millis(1700000000000) == 1700000000000
    esperado = int(datetime.fromisoformat("2026-09-18T07:05:03-03:00").timestamp() * 1000)
    assert para_epoch_millis("2026-09-18T07:05:03-03:00") == esperado
    assert para_epoch_millis("2026-09-18T10:05:03Z") == esperado   # mesmo instante
    assert para_epoch_millis(None) is None
    assert para_epoch_millis("nada disso") is None


def test_backend_manda_na_lista_e_config_completa_a_senha():
    do_config = [{"id": "7", "nome": "local", "host": "192.168.0.10", "port": 80,
                  "username": "admin", "password": "senha-do-leitor", "modelo": "x",
                  "door_id": 1, "ativo": True}]
    do_backend = [{"id": 7, "nome": "PORTARIA", "ip": "192.168.0.10"}]
    lista = mesclar_dispositivos(do_config, do_backend)
    assert len(lista) == 1
    assert lista[0]["nome"] == "PORTARIA"          # o backend nomeia
    assert lista[0]["password"] == "senha-do-leitor"  # o config guarda a senha


def test_equipamento_so_no_config_continua_valendo():
    """Backend sem a rota /devices (404) nao pode apagar o leitor da escola."""
    do_config = [{"id": "7", "nome": "PORTARIA", "host": "192.168.0.10", "port": 80,
                  "username": "admin", "password": "s", "modelo": "x", "door_id": 1,
                  "ativo": True}]
    assert len(mesclar_dispositivos(do_config, [])) == 1


def test_equipamento_do_backend_sem_ip_e_ignorado():
    do_backend = [{"id": 99, "nome": "SEM IP"}]
    assert mesclar_dispositivos([], do_backend) == []


def test_tipo_e_payload_da_tarefa_toleram_grafias():
    assert tipo_da_tarefa({"tipo": "Abrir_Acesso"}) == "abrir_acesso"
    assert tipo_da_tarefa({"type": "sincronizar_pessoa"}) == "sincronizar_pessoa"
    assert tipo_da_tarefa({}) == ""
    assert payload_da_tarefa({"payload": {"a": 1}}) == {"a": 1}
    assert payload_da_tarefa({"params": '{"a": 2}'}) == {"a": 2}
    assert payload_da_tarefa({"deviceId": "7"}) == {"deviceId": "7"}
