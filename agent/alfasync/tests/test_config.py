"""Configuracao: validacao com mensagem que o tecnico da escola entende."""
import json

import pytest

import config as config_mod
from config import ConfigError, carregar

BASE = {
    "backend": {
        "base_url": "https://escola.exemplo",
        "client_id": "uuid-1",
        "username": "SYNC-1",
        "password": "segredo",
    },
    "devices": [{"id": "1", "nome": "PORTARIA", "host": "192.168.0.10",
                 "password": "senha-leitor"}],
}


def escrever(tmp_path, dados):
    caminho = tmp_path / "config.json"
    caminho.write_text(json.dumps(dados), encoding="utf-8")
    return str(caminho)


def test_carrega_e_aplica_padroes(tmp_path):
    cfg = carregar(escrever(tmp_path, BASE))
    assert cfg.backend_url == "https://escola.exemplo"
    assert cfg.intervalo_heartbeat == 5
    assert cfg.intervalo_fila == 15
    assert cfg.fila_retencao_horas == 72
    assert cfg.devices[0]["port"] == 80
    assert cfg.devices[0]["username"] == "admin"


def test_campo_obrigatorio_ausente_diz_qual(tmp_path):
    dados = json.loads(json.dumps(BASE))
    del dados["backend"]["client_id"]
    with pytest.raises(ConfigError) as erro:
        carregar(escrever(tmp_path, dados))
    assert "backend.client_id" in str(erro.value)


def test_campo_vazio_tambem_falha(tmp_path):
    dados = json.loads(json.dumps(BASE))
    dados["backend"]["password"] = "   "
    with pytest.raises(ConfigError) as erro:
        carregar(escrever(tmp_path, dados))
    assert "backend.password" in str(erro.value)


def test_url_sem_esquema_falha(tmp_path):
    dados = json.loads(json.dumps(BASE))
    dados["backend"]["base_url"] = "escola.exemplo"
    with pytest.raises(ConfigError) as erro:
        carregar(escrever(tmp_path, dados))
    assert "http" in str(erro.value)


def test_equipamento_sem_host_falha(tmp_path):
    dados = json.loads(json.dumps(BASE))
    dados["devices"][0].pop("host")
    with pytest.raises(ConfigError) as erro:
        carregar(escrever(tmp_path, dados))
    assert "host" in str(erro.value)


def test_json_invalido_aponta_a_linha(tmp_path):
    caminho = tmp_path / "config.json"
    caminho.write_text('{"backend": }', encoding="utf-8")
    with pytest.raises(ConfigError) as erro:
        carregar(str(caminho))
    assert "JSON" in str(erro.value)


def test_arquivo_ausente_explica_o_que_fazer(tmp_path):
    with pytest.raises(ConfigError) as erro:
        carregar(str(tmp_path / "nao-existe.json"))
    assert "config.example.json" in str(erro.value)


def test_exemplo_nao_contem_credencial():
    """O arquivo versionado nao pode ter senha de ninguem."""
    import os

    caminho = os.path.join(config_mod.diretorio_base(), "config.example.json")
    with open(caminho, encoding="utf-8") as fh:
        exemplo = json.load(fh)
    assert exemplo["backend"]["password"] == "PREENCHER"
    assert exemplo["backend"]["username"] == "PREENCHER"
    assert exemplo["devices"][0]["password"] == "PREENCHER"
    # E precisa ser um esqueleto valido: so faltam as credenciais.
    assert set(exemplo["backend"]) >= {"base_url", "client_id", "username", "password"}
