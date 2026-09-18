"""Configuracao do gateway, lida de um JSON ao lado do executavel.

Nada de credencial em codigo: tudo que identifica a escola (clientId, usuario,
senha do backend e senha dos leitores) mora no `config.json` da maquina, que nao
vai para o repositorio. O `config.example.json` e so o esqueleto.

A validacao acontece na carga e falha cedo, com a mensagem dizendo exatamente
qual campo falta: quem instala isso e o tecnico na escola, muitas vezes por
telefone — "campo obrigatorio ausente: backend.client_id" resolve a ligacao,
"KeyError" nao.
"""
import json
import logging
import os
import sys
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)

NOME_ARQUIVO = "config.json"

# Intervalos padrao (segundos). Sao os do enunciado; o config pode sobrepor.
INTERVALOS_PADRAO = {
    "heartbeat_s": 5,
    "eventos_s": 5,
    "fila_s": 15,
    "pessoas_s": 30,
    "tarefas_s": 10,
}

FILA_PADRAO = {
    "lote": 50,
    "retencao_horas": 72,
    "backoff_base_s": 15,
    "backoff_teto_s": 900,
}

LOG_PADRAO = {
    "nivel": "INFO",
    "arquivo": "alfasync.log",
    "max_bytes": 5 * 1024 * 1024,
    "backups": 5,
}

TIMEOUTS_PADRAO = {
    "backend_s": 30,
    "dispositivo_s": 8,
}


class ConfigError(Exception):
    """Configuracao invalida — o agente nao sobe."""


def diretorio_base() -> str:
    """Onde ficam config.json, state.json, fila e log.

    Ao lado do executavel quando empacotado (PyInstaller), ao lado do modulo
    quando rodando do fonte. O mesmo criterio dos agentes irmaos: esses arquivos
    precisam sobreviver a uma atualizacao que troque a pasta do programa.
    """
    if getattr(sys, "frozen", False):
        return os.path.dirname(sys.executable)
    return os.path.dirname(os.path.abspath(__file__))


def caminho_padrao() -> str:
    """Caminho do config.json. Variavel de ambiente ganha (util em teste e servico)."""
    return os.environ.get("ALFASYNC_CONFIG") or os.path.join(diretorio_base(), NOME_ARQUIVO)


def _exigir(dados: Dict[str, Any], caminho: str) -> Any:
    """Le `a.b.c` de um dict aninhado ou levanta ConfigError com o caminho completo."""
    atual: Any = dados
    for parte in caminho.split("."):
        if not isinstance(atual, dict) or parte not in atual:
            raise ConfigError(
                f"Campo obrigatorio ausente no {NOME_ARQUIVO}: '{caminho}'. "
                f"Use o config.example.json como referencia."
            )
        atual = atual[parte]
    if atual is None or (isinstance(atual, str) and not atual.strip()):
        raise ConfigError(
            f"Campo obrigatorio vazio no {NOME_ARQUIVO}: '{caminho}'."
        )
    return atual


def _com_padroes(valor: Optional[Dict[str, Any]], padrao: Dict[str, Any]) -> Dict[str, Any]:
    resultado = dict(padrao)
    if isinstance(valor, dict):
        for chave, v in valor.items():
            if v is not None:
                resultado[chave] = v
    return resultado


def normalizar_device(bruto: Dict[str, Any]) -> Dict[str, Any]:
    """Traduz um equipamento (do backend ou do config) para o formato interno.

    Aceita varias grafias de propósito: o lado servidor esta sendo escrito em
    paralelo e pode nomear os campos em portugues ou ingles. Um gateway que
    quebra porque o backend mandou `ip` em vez de `host` nao serve para nada.
    """
    def pega(*nomes, padrao=None):
        for nome in nomes:
            if nome in bruto and bruto[nome] not in (None, ""):
                return bruto[nome]
        return padrao

    ident = pega("id", "deviceId", "device_id", "identificador")
    return {
        "id": str(ident) if ident is not None else "",
        "nome": str(pega("nome", "name", "descricao", "description", padrao=f"Leitor {ident}")),
        "host": str(pega("host", "ip", "endereco", "address", padrao="")),
        "port": int(pega("port", "porta", padrao=80)),
        "username": str(pega("username", "usuario", "login", padrao="admin")),
        "password": str(pega("password", "senha", padrao="")),
        "modelo": str(pega("modelo", "model", "tipo", "type", padrao="controlid_idface")),
        "door_id": int(pega("door_id", "doorId", "porta_rele", padrao=1)),
        "ativo": bool(pega("ativo", "active", "enabled", padrao=True)),
    }


class Config:
    """Configuracao validada. Os atributos sao o contrato usado pelo resto do agente."""

    def __init__(self, dados: Dict[str, Any], origem: str = ""):
        self.origem = origem
        self.bruto = dados

        self.backend_url = str(_exigir(dados, "backend.base_url")).rstrip("/")
        self.client_id = str(_exigir(dados, "backend.client_id"))
        self.username = str(_exigir(dados, "backend.username"))
        self.password = str(_exigir(dados, "backend.password"))
        self.verify_ssl = bool(dados.get("backend", {}).get("verify_ssl", True))

        if not self.backend_url.startswith(("http://", "https://")):
            raise ConfigError(
                f"'backend.base_url' precisa comecar com http:// ou https:// "
                f"(recebido: {self.backend_url!r})."
            )

        # Equipamentos locais: opcionais. A lista autoritativa vem de
        # GET /agent/devices; o que esta aqui serve de fallback (backend fora do
        # ar na primeira subida) e para guardar a senha do leitor, que o backend
        # pode nao conhecer.
        devices = dados.get("devices") or []
        if not isinstance(devices, list):
            raise ConfigError("'devices' precisa ser uma lista (pode ser vazia).")
        self.devices: List[Dict[str, Any]] = [normalizar_device(d) for d in devices]
        for d in self.devices:
            if not d["host"]:
                raise ConfigError(
                    f"Equipamento '{d['nome']}' sem 'host' (IP do leitor na rede da escola)."
                )

        intervalos = _com_padroes(dados.get("intervalos"), INTERVALOS_PADRAO)
        self.intervalo_heartbeat = float(intervalos["heartbeat_s"])
        self.intervalo_eventos = float(intervalos["eventos_s"])
        self.intervalo_fila = float(intervalos["fila_s"])
        self.intervalo_pessoas = float(intervalos["pessoas_s"])
        self.intervalo_tarefas = float(intervalos["tarefas_s"])

        fila = _com_padroes(dados.get("fila"), FILA_PADRAO)
        self.fila_lote = int(fila["lote"])
        self.fila_retencao_horas = int(fila["retencao_horas"])
        self.backoff_base_s = float(fila["backoff_base_s"])
        self.backoff_teto_s = float(fila["backoff_teto_s"])

        log = _com_padroes(dados.get("log"), LOG_PADRAO)
        self.log_nivel = str(log["nivel"]).upper()
        if self.log_nivel not in ("DEBUG", "INFO", "WARNING", "ERROR"):
            raise ConfigError(
                f"'log.nivel' invalido: {self.log_nivel!r}. Use DEBUG, INFO, WARNING ou ERROR."
            )
        self.log_arquivo = str(log["arquivo"])
        if not os.path.isabs(self.log_arquivo):
            self.log_arquivo = os.path.join(diretorio_base(), self.log_arquivo)
        self.log_max_bytes = int(log["max_bytes"])
        self.log_backups = int(log["backups"])

        timeouts = _com_padroes(dados.get("timeouts"), TIMEOUTS_PADRAO)
        self.timeout_backend = float(timeouts["backend_s"])
        # Timeout curto no leitor: um equipamento mudo nao pode segurar o ciclo.
        self.timeout_dispositivo = float(timeouts["dispositivo_s"])

        # TTL da sessao do leitor. O firmware expira a sessao sozinho; renovar
        # antes disso evita um 401 por operacao.
        self.sessao_ttl_s = float(dados.get("dispositivo_sessao_ttl_s", 300))

        self.eventos_lote = int(dados.get("eventos_lote", 200))

        base = dados.get("dados_dir") or diretorio_base()
        self.dados_dir = base if os.path.isabs(base) else os.path.join(diretorio_base(), base)
        self.state_path = os.path.join(self.dados_dir, "state.json")
        self.fila_path = os.path.join(self.dados_dir, "event_queue.db")


def carregar(caminho: Optional[str] = None) -> Config:
    """Le e valida o config.json. Levanta ConfigError com mensagem de gente."""
    caminho = caminho or caminho_padrao()
    if not os.path.exists(caminho):
        raise ConfigError(
            f"Arquivo de configuracao nao encontrado: {caminho}\n"
            f"Copie o config.example.json para {NOME_ARQUIVO} e preencha as credenciais."
        )
    try:
        with open(caminho, "r", encoding="utf-8") as fh:
            dados = json.load(fh)
    except json.JSONDecodeError as e:
        raise ConfigError(
            f"{caminho} nao e um JSON valido (linha {e.lineno}, coluna {e.colno}): {e.msg}"
        )
    except OSError as e:
        raise ConfigError(f"Nao foi possivel ler {caminho}: {e}")

    if not isinstance(dados, dict):
        raise ConfigError(f"{caminho} precisa conter um objeto JSON no topo.")

    return Config(dados, origem=caminho)
