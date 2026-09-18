"""Relogio: o firmware Control iD conta epoch em horario LOCAL, nao em UTC.

O leitor grava em `access_logs.time` o resultado de "quantos segundos desde 1970"
calculado sobre os componentes do relogio de parede dele (America/Sao_Paulo).
Ou seja: o numero que chega ja esta deslocado do epoch verdadeiro por uma hora de
fuso inteira. Ler esse valor com `datetime.fromtimestamp(...)` (que assume o fuso
do sistema) joga o acesso tres horas para tras — e a apuracao de permanencia do
aluno sai errada em todo turno da manha, com evento caindo no dia anterior.

Por isso a conversao aqui e explicita: le os componentes como se fossem UTC
(porque e assim que o leitor os empacotou) e carimba o offset de Sao Paulo.

Offset fixo -03:00 de proposito: o Brasil nao tem mais horario de verao desde o
Decreto 9.772/2019, e depender de `zoneinfo` exigiria o pacote `tzdata`
instalado na maquina da escola (nem sempre presente em container slim ou em
Windows). Se o horario de verao voltar, este e o unico ponto a mudar.
"""
import logging
import time as _time
from datetime import datetime, timedelta, timezone
from typing import Optional

logger = logging.getLogger(__name__)

# America/Sao_Paulo, sem horario de verao.
TZ_ESCOLA = timezone(timedelta(hours=-3))

# Acima disso o relogio do leitor esta errado o bastante para corromper apuracao.
LIMITE_DESVIO_SEGUNDOS = 120


def epoch_local_para_datetime(epoch: int) -> datetime:
    """Converte o epoch "local" do leitor no instante real, com fuso.

    `datetime.fromtimestamp(epoch, timezone.utc)` devolve exatamente os
    componentes de data/hora que o leitor mostrou na tela; trocar o tzinfo por
    -03:00 (sem mexer nos componentes) transforma isso no instante correto.
    """
    componentes = datetime.fromtimestamp(int(epoch), timezone.utc)
    return componentes.replace(tzinfo=TZ_ESCOLA)


def epoch_local_para_iso(epoch: int) -> str:
    """ISO-8601 com offset, no formato que o backend espera: 2026-09-18T07:05:03-03:00."""
    return epoch_local_para_datetime(epoch).isoformat()


def agora_iso() -> str:
    """Instante atual do gateway em ISO-8601 com offset.

    Usado quando o evento vem sem `time` — e melhor registrar a passagem com o
    horario do gateway do que descartar a passagem.
    """
    return datetime.now(TZ_ESCOLA).replace(microsecond=0).isoformat()


def desvio_do_relogio(epoch_leitor: int, agora_epoch: Optional[float] = None) -> float:
    """Quantos segundos o relogio do leitor esta adiantado em relacao ao gateway.

    Positivo = leitor adiantado. Negativo = leitor atrasado.
    """
    if agora_epoch is None:
        agora_epoch = _time.time()
    instante_real = epoch_local_para_datetime(epoch_leitor).timestamp()
    return instante_real - agora_epoch


def checar_relogio(nome_dispositivo: str, epoch_leitor: int,
                   agora_epoch: Optional[float] = None,
                   limite: int = LIMITE_DESVIO_SEGUNDOS) -> float:
    """Loga WARN quando o leitor esta adiantado alem do limite. Devolve o desvio.

    So o adiantamento vira WARN: leitor adiantado registra saida antes da entrada
    e estoura a permanencia; leitor atrasado e sintoma mais raro (bateria do RTC)
    e fica em DEBUG para nao poluir o log de uma escola inteira.
    """
    desvio = desvio_do_relogio(epoch_leitor, agora_epoch)
    if desvio > limite:
        logger.warning(
            f"{nome_dispositivo}: relogio do leitor adiantado {int(desvio)}s "
            f"({desvio / 60:.1f} min) em relacao ao gateway — "
            "acerte a hora do equipamento; a apuracao de permanencia depende dela."
        )
    elif desvio < -limite:
        logger.debug(f"{nome_dispositivo}: relogio do leitor atrasado {int(-desvio)}s.")
    return desvio
