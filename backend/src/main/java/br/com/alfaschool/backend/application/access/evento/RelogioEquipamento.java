package br.com.alfaschool.backend.application.access.evento;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Conversao de tempo do firmware Control iD.
 *
 * O EQUIPAMENTO NAO REPORTA EPOCH UTC. Ele conta os segundos desde
 * 1970-01-01 00:00:00 usando o relogio LOCAL do aparelho. Ou seja: o
 * numero que chega ja esta "no fuso da escola" e interpreta-lo como UTC
 * joga todo evento 3 horas para frente.
 *
 * A conversao correta e' a de duas etapas: ler o numero como se fosse
 * UTC para recuperar o LocalDateTime que o aparelho quis dizer, e so'
 * entao ancorar esse LocalDateTime no fuso configurado.
 *
 * O Brasil nao tem mais horario de verao desde 2019, entao a America/
 * Sao_Paulo e' um offset fixo -03:00 para datas atuais. A implementacao
 * ainda assim passa pelo ZoneId em vez de somar 3 horas na mao: dados
 * historicos anteriores a 2019 caem dentro do DST e o offset era -02:00.
 */
public final class RelogioEquipamento {

    private RelogioEquipamento() {
    }

    /**
     * Tolerancia para relogio adiantado. Acima disso o horario do leitor
     * esta errado (bateria da placa, NTP mal configurado) e usar o valor
     * cru colocaria o evento no futuro, onde nenhum relatorio o encontra.
     */
    public static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);

    /** Epoch calculado no horario local do equipamento -> instante real. */
    public static Instant doEpochLocal(long epochLocal, ZoneId zonaDoEquipamento) {
        LocalDateTime relogioDoAparelho = LocalDateTime.ofEpochSecond(epochLocal, 0, ZoneOffset.UTC);
        return relogioDoAparelho.atZone(zonaDoEquipamento).toInstant();
    }

    /** Caminho inverso, para montar filtros de where que o firmware entenda. */
    public static long paraEpochLocal(Instant instante, ZoneId zonaDoEquipamento) {
        return instante.atZone(zonaDoEquipamento).toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * Timestamp no FUTURO alem da tolerancia e' grampeado na hora do
     * servidor: o relogio do leitor esta errado.
     *
     * Timestamp no PASSADO e' aceito sem ajuste, sempre. Fila offline do
     * agente reenviando um dia inteiro de leituras e' comportamento
     * esperado, e corrigir isso apagaria a hora real das entradas.
     */
    public static Instant clampar(Instant lido, Instant agora) {
        if (lido == null) {
            return agora;
        }
        return lido.isAfter(agora.plus(TOLERANCIA_FUTURO)) ? agora : lido;
    }

    public static boolean noFuturo(Instant lido, Instant agora) {
        return lido != null && lido.isAfter(agora.plus(TOLERANCIA_FUTURO));
    }
}
