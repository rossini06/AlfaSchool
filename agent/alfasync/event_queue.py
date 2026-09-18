"""Fila persistente de eventos (SQLite).

A internet da escola cai. Quando cai, o leitor continua registrando gente
passando, e cada passagem perdida e um aluno que "nao entrou" no relatorio. Por
isso a fila e em disco, e nao em memoria: ela sobrevive a queda de rede, a queda
de energia e ao reinicio do gateway.

Duas garantias que vieram de bug real em producao:

* `UNIQUE(device_id, ext_id)` + `INSERT OR IGNORE`: idempotencia no proprio
  gateway. O mesmo evento pode ser reprocessado (o cursor do leitor so avanca
  quando a entrega confirma) e nao pode virar duas linhas na fila.
* backoff por tentativa: sem ele, uma fila com mil eventos e o backend fora do ar
  vira mil requisicoes a cada 15 segundos.

O que NAO esta aqui, de proposito: a decisao de descartar um evento. Quem
classifica o erro e o backend_api; quem aplica a politica e o agent
(`drenar_fila`). A fila so guarda.
"""
import json
import logging
import os
import sqlite3
import threading
import time
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)

BACKOFF_BASE_PADRAO = 15.0
BACKOFF_TETO_PADRAO = 900.0   # 15 min: acima disso o reenvio fica lento demais


def proximo_intervalo(attempts: int, base: float = BACKOFF_BASE_PADRAO,
                      teto: float = BACKOFF_TETO_PADRAO) -> float:
    """Backoff exponencial simples: base * 2^(attempts-1), limitado ao teto.

    Sem tentativa nenhuma (attempts=0) o evento sai na hora — a primeira tentativa
    de reenvio nao pode esperar.
    """
    if attempts <= 0:
        return 0.0
    return min(teto, base * (2 ** (attempts - 1)))


class EventQueue:
    """Fila thread-safe em SQLite, autocommit.

    Uma unica conexao persistente (check_same_thread=False + lock interno):
    funciona com `:memory:` nos testes e evita reabrir o arquivo a cada operacao.
    """

    def __init__(self, db_path: str, backoff_base: float = BACKOFF_BASE_PADRAO,
                 backoff_teto: float = BACKOFF_TETO_PADRAO):
        self._lock = threading.Lock()
        self._db = db_path
        self.backoff_base = backoff_base
        self.backoff_teto = backoff_teto

        if db_path != ":memory:":
            pasta = os.path.dirname(os.path.abspath(db_path))
            if pasta:
                os.makedirs(pasta, exist_ok=True)

        self._conn = sqlite3.connect(db_path, timeout=10, check_same_thread=False)
        self._conn.isolation_level = None   # autocommit: cada execute e sua propria tx
        self._criar_tabela()

    def _criar_tabela(self) -> None:
        with self._lock:
            self._conn.execute("""
                CREATE TABLE IF NOT EXISTS pending_events (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    device_id  TEXT NOT NULL,
                    ext_id     TEXT NOT NULL,
                    payload    TEXT NOT NULL,
                    attempts   INTEGER NOT NULL DEFAULT 0,
                    created_at REAL NOT NULL,
                    last_tried REAL,
                    ultimo_erro TEXT,
                    UNIQUE(device_id, ext_id)
                )
            """)
            self._conn.execute(
                "CREATE INDEX IF NOT EXISTS ix_pending_ordem ON pending_events(id)"
            )

    # ─── operacoes ────────────────────────────────────────────────────────────

    def enfileirar(self, device_id: str, ext_id: str, payload: Dict[str, Any]) -> bool:
        """INSERT OR IGNORE. True se inseriu, False se o par ja estava na fila."""
        with self._lock:
            cur = self._conn.execute(
                "INSERT OR IGNORE INTO pending_events(device_id, ext_id, payload, created_at) "
                "VALUES (?, ?, ?, ?)",
                (str(device_id), str(ext_id), json.dumps(payload), time.time()),
            )
            return cur.rowcount == 1

    def proximos(self, limite: int = 50, agora: Optional[float] = None) -> List[Dict[str, Any]]:
        """Os proximos eventos prontos para tentativa, do mais antigo para o mais novo.

        "Pronto" = nunca tentado, ou ja passou o backoff da ultima tentativa.
        A ordem por id preserva a sequencia das passagens.
        """
        agora = time.time() if agora is None else agora
        with self._lock:
            self._conn.row_factory = sqlite3.Row
            try:
                linhas = self._conn.execute(
                    "SELECT * FROM pending_events ORDER BY id ASC"
                ).fetchall()
            finally:
                self._conn.row_factory = None

        prontos = []
        for linha in linhas:
            registro = dict(linha)
            ultima = registro.get("last_tried")
            espera = proximo_intervalo(registro.get("attempts", 0),
                                       self.backoff_base, self.backoff_teto)
            if ultima is None or (agora - ultima) >= espera:
                prontos.append(registro)
            if len(prontos) >= limite:
                break
        return prontos

    def marcar_enviado(self, row_id: int) -> None:
        with self._lock:
            self._conn.execute("DELETE FROM pending_events WHERE id = ?", (row_id,))

    def marcar_falha(self, row_id: int, erro: str = "",
                     agora: Optional[float] = None) -> None:
        """Conta a tentativa e guarda o motivo. O evento CONTINUA na fila."""
        agora = time.time() if agora is None else agora
        with self._lock:
            self._conn.execute(
                "UPDATE pending_events SET attempts = attempts + 1, last_tried = ?, "
                "ultimo_erro = ? WHERE id = ?",
                (agora, (erro or "")[:300], row_id),
            )

    def remover_por_ext(self, device_id: str, ext_id: str) -> None:
        """Tira da fila o evento entregue direto, sem passar pelo reenvio.

        A coleta enfileira ANTES de tentar o envio (para nao perder a passagem se
        o processo morrer no meio); quando o envio da certo na hora, a linha sai
        por aqui.
        """
        with self._lock:
            self._conn.execute(
                "DELETE FROM pending_events WHERE device_id = ? AND ext_id = ?",
                (str(device_id), str(ext_id)),
            )

    def descartar(self, row_id: int, motivo: str = "") -> None:
        """Tira o evento da fila em recusa definitiva.

        Existe separado de `marcar_enviado` so pela leitura do codigo: sao a mesma
        operacao no banco, mas significados opostos — um entregou, o outro desistiu.
        """
        if motivo:
            logger.warning(f"Evento descartado da fila (recusa definitiva do backend): {motivo}")
        self.marcar_enviado(row_id)

    def total(self) -> int:
        with self._lock:
            return self._conn.execute("SELECT COUNT(*) FROM pending_events").fetchone()[0]

    def prune_old(self, horas: int = 72) -> int:
        """Remove o que passou de `horas` na fila. Devolve quantos sairam.

        Sem isso o banco cresce sem limite numa escola que ficou semanas sem
        internet, e o gateway passa a reenviar historico antigo demais para ter
        valor. 72h e o padrao: cobre um fim de semana prolongado.
        """
        corte = time.time() - horas * 3600
        with self._lock:
            cur = self._conn.execute(
                "DELETE FROM pending_events WHERE created_at < ?", (corte,)
            )
            removidos = cur.rowcount or 0
        if removidos:
            logger.warning(f"Fila: {removidos} evento(s) com mais de {horas}h descartado(s).")
        return removidos

    def fechar(self) -> None:
        with self._lock:
            try:
                self._conn.close()
            except Exception:
                pass
