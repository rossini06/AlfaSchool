package br.com.alfaschool.backend.access.evento;

import br.com.alfaschool.backend.application.access.evento.RelogioEquipamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversao do epoch "local" do firmware Control iD.
 *
 * O firmware conta segundos desde 1970 usando o relogio LOCAL do
 * aparelho. Tratar isso como epoch UTC joga todo evento 3 horas para
 * frente — o bug que fazia a entrada das 7h20 aparecer as 10h20.
 */
class RelogioEquipamentoTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    /** Numero que o equipamento manda quando o relogio dele marca a hora dada. */
    private static long comoOFirmwareCalcula(int ano, int mes, int dia, int hora, int minuto) {
        return LocalDateTime.of(ano, mes, dia, hora, minuto).toEpochSecond(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("epoch local do firmware vira o instante real, nao 3 horas adiantado")
    void converteEpochLocal() {
        long doEquipamento = comoOFirmwareCalcula(2026, 3, 15, 7, 20);

        Instant convertido = RelogioEquipamento.doEpochLocal(doEquipamento, SP);

        assertThat(convertido)
                .isEqualTo(ZonedDateTime.of(2026, 3, 15, 7, 20, 0, 0, SP).toInstant());
        // A leitura das 7h20 continua sendo 7h20 quando lida de volta no fuso da escola.
        assertThat(convertido.atZone(SP).getHour()).isEqualTo(7);
        // E NAO e' o instante que sairia de uma leitura ingenua como UTC.
        assertThat(convertido).isNotEqualTo(Instant.ofEpochSecond(doEquipamento));
    }

    @Test
    @DisplayName("janeiro usa -03:00: o Brasil nao tem mais horario de verao desde 2019")
    void semHorarioDeVeraoDepoisDe2019() {
        long doEquipamento = comoOFirmwareCalcula(2026, 1, 15, 8, 0);

        Instant convertido = RelogioEquipamento.doEpochLocal(doEquipamento, SP);

        // 08:00 local -03:00 == 11:00 UTC. Se o codigo somasse um DST que
        // nao existe mais, daria 10:00 UTC.
        assertThat(convertido.atZone(ZoneOffset.UTC).getHour()).isEqualTo(11);
    }

    @Test
    @DisplayName("data anterior a 2019 respeita o horario de verao que existia na epoca")
    void horarioDeVeraoHistorico() {
        long doEquipamento = comoOFirmwareCalcula(2018, 1, 15, 8, 0);

        Instant convertido = RelogioEquipamento.doEpochLocal(doEquipamento, SP);

        // Em janeiro de 2018 Sao Paulo estava em -02:00: 08:00 local == 10:00 UTC.
        // E' por isso que a conversao passa pelo ZoneId em vez de somar 3 horas.
        assertThat(convertido.atZone(ZoneOffset.UTC).getHour()).isEqualTo(10);
    }

    @Test
    @DisplayName("virada de dia: 23h50 local cai no dia seguinte em UTC")
    void viradaDeDia() {
        long doEquipamento = comoOFirmwareCalcula(2026, 6, 30, 23, 50);

        Instant convertido = RelogioEquipamento.doEpochLocal(doEquipamento, SP);

        assertThat(convertido.atZone(SP).getDayOfMonth()).isEqualTo(30);
        assertThat(convertido.atZone(ZoneOffset.UTC).getDayOfMonth()).isEqualTo(1);
        assertThat(convertido.atZone(ZoneOffset.UTC).getHour()).isEqualTo(2);
    }

    @Test
    @DisplayName("ida e volta preserva o numero que o equipamento entende")
    void idaEVolta() {
        long original = comoOFirmwareCalcula(2026, 9, 18, 14, 33);

        long reconvertido = RelogioEquipamento.paraEpochLocal(
                RelogioEquipamento.doEpochLocal(original, SP), SP);

        assertThat(reconvertido).isEqualTo(original);
    }

    @Test
    @DisplayName("timestamp mais de 5 minutos no futuro e' grampeado na hora do servidor")
    void futuroAlemDaToleranciaEhGrampeado() {
        Instant agora = Instant.parse("2026-09-18T12:00:00Z");
        Instant relogioAdiantado = agora.plusSeconds(3600);

        assertThat(RelogioEquipamento.noFuturo(relogioAdiantado, agora)).isTrue();
        assertThat(RelogioEquipamento.clampar(relogioAdiantado, agora)).isEqualTo(agora);
    }

    @Test
    @DisplayName("futuro dentro da tolerancia passa intacto")
    void futuroDentroDaToleranciaPassa() {
        Instant agora = Instant.parse("2026-09-18T12:00:00Z");
        Instant poucoAdiantado = agora.plusSeconds(120);

        assertThat(RelogioEquipamento.noFuturo(poucoAdiantado, agora)).isFalse();
        assertThat(RelogioEquipamento.clampar(poucoAdiantado, agora)).isEqualTo(poucoAdiantado);
    }

    @Test
    @DisplayName("passado e' aceito sem ajuste: fila offline do agente e' legitima")
    void passadoEhAceito() {
        Instant agora = Instant.parse("2026-09-18T12:00:00Z");
        Instant ontem = agora.minusSeconds(86_400);

        assertThat(RelogioEquipamento.noFuturo(ontem, agora)).isFalse();
        assertThat(RelogioEquipamento.clampar(ontem, agora)).isEqualTo(ontem);
    }
}
