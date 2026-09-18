"""Relogio do leitor: epoch local → ISO com offset, e deteccao de desvio."""
import calendar
import logging
from datetime import datetime

import clock


def epoch_do_firmware(ano, mes, dia, hora, minuto, segundo) -> int:
    """Reproduz o que o firmware faz: epoch calculado sobre o relogio de parede.

    O leitor conta os segundos desde 1970 usando os componentes LOCAIS, sem
    aplicar o fuso — e exatamente isso que `timegm` faz.
    """
    return calendar.timegm((ano, mes, dia, hora, minuto, segundo, 0, 0, 0))


def test_epoch_local_vira_iso_com_offset_de_sao_paulo():
    epoch = epoch_do_firmware(2026, 9, 18, 7, 5, 3)
    assert clock.epoch_local_para_iso(epoch) == "2026-09-18T07:05:03-03:00"


def test_virada_de_dia_para_a_frente():
    """00:30 no leitor e 00:30 do dia seguinte, nao 21:30 do dia anterior.

    Ler o epoch do firmware como se fosse UTC real joga o acesso tres horas para
    tras — o evento cai no dia anterior e a apuracao de permanencia do aluno
    fecha errada.
    """
    epoch = epoch_do_firmware(2026, 9, 19, 0, 30, 0)
    assert clock.epoch_local_para_iso(epoch) == "2026-09-19T00:30:00-03:00"

    errado = datetime.fromtimestamp(epoch, clock.TZ_ESCOLA).isoformat()
    assert errado == "2026-09-18T21:30:00-03:00"
    assert clock.epoch_local_para_iso(epoch) != errado


def test_virada_de_dia_para_tras():
    epoch = epoch_do_firmware(2026, 9, 18, 23, 40, 15)
    assert clock.epoch_local_para_iso(epoch) == "2026-09-18T23:40:15-03:00"


def test_instante_real_corresponde_ao_relogio_de_parede():
    """O instante convertido, visto em UTC, e o horario local + 3h."""
    epoch = epoch_do_firmware(2026, 9, 18, 7, 0, 0)
    momento = clock.epoch_local_para_datetime(epoch)
    assert momento.utcoffset().total_seconds() == -3 * 3600
    assert momento.astimezone(clock.TZ_ESCOLA).hour == 7
    # Epoch verdadeiro do instante = valor do firmware + 3h de fuso.
    assert momento.timestamp() == epoch + 3 * 3600


def test_desvio_zero_quando_os_relogios_batem():
    epoch = epoch_do_firmware(2026, 9, 18, 7, 0, 0)
    agora_real = epoch + 3 * 3600      # o mesmo instante, no relogio do gateway
    assert clock.desvio_do_relogio(epoch, agora_real) == 0


def test_desvio_positivo_quando_o_leitor_esta_adiantado():
    epoch = epoch_do_firmware(2026, 9, 18, 7, 5, 0)   # leitor marcando 07:05
    agora_real = epoch_do_firmware(2026, 9, 18, 7, 0, 0) + 3 * 3600   # gateway em 07:00
    assert clock.desvio_do_relogio(epoch, agora_real) == 300


def test_warn_quando_o_leitor_passa_de_dois_minutos_adiantado(caplog):
    epoch = epoch_do_firmware(2026, 9, 18, 7, 5, 0)
    agora_real = epoch_do_firmware(2026, 9, 18, 7, 0, 0) + 3 * 3600
    with caplog.at_level(logging.WARNING, logger="clock"):
        desvio = clock.checar_relogio("PORTARIA", epoch, agora_real)
    assert desvio == 300
    assert any("adiantado" in r.message for r in caplog.records)


def test_sem_warn_dentro_da_tolerancia(caplog):
    epoch = epoch_do_firmware(2026, 9, 18, 7, 0, 30)
    agora_real = epoch_do_firmware(2026, 9, 18, 7, 0, 0) + 3 * 3600
    with caplog.at_level(logging.WARNING, logger="clock"):
        clock.checar_relogio("PORTARIA", epoch, agora_real)
    assert not [r for r in caplog.records if r.levelno >= logging.WARNING]


def test_agora_iso_tem_offset():
    assert clock.agora_iso().endswith("-03:00")
