package br.com.alfaschool.backend.application.access.permanencia;

import br.com.alfaschool.backend.application.access.permanencia.EventoAcessoLeitor.EventoAcesso;
import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nucleo aritmetico da apuracao de permanencia. Sem Spring, sem banco,
 * sem relogio proprio: tudo entra por parametro para que a conta que vira
 * cobranca possa ser reproduzida numa planilha e num teste.
 *
 * <h2>Pareamento</h2>
 * Os eventos do dia sao lidos em ordem cronologica e pareados por
 * POSICAO — 1a entrada, 1a saida, 2a entrada, 2a saida — e nao pelo campo
 * {@code sentido} do equipamento. Leitor mal configurado grava
 * INDEFINIDO ou o sentido trocado; a ordem do relogio, nao.
 *
 * <h2>Excedente: DURACAO e HORARIO sao contas DIFERENTES</h2>
 * A escola precisa escolher qual contratou, porque elas divergem no caso
 * mais comum da portaria. Aluno contratado das 07h as 17h (600 min) que
 * chega as 08h e sai as 18h:
 * <ul>
 *   <li>permaneceu 600 min — exatamente o contratado, entao por
 *       {@code DURACAO} o excedente e' ZERO;</li>
 *   <li>saiu 60 min depois da saida prevista, entao por {@code HORARIO}
 *       o excedente e' 60 min;</li>
 *   <li>por {@code AMBOS} vale o maior: 60 min.</li>
 * </ul>
 * Quem cobra por hora de creche usa DURACAO; quem cobra porque a
 * funcionaria teve de ficar ate o aluno sair usa HORARIO.
 *
 * <h2>Tolerancia</h2>
 * {@code toleranciaSaidaMin} e' franquia do EXCEDENTE (e da antecipacao).
 * {@code toleranciaEntradaMin} e' franquia do ATRASO de entrada e nao
 * entra na conta do excedente — sao coisas diferentes e somar as duas
 * dobraria a franquia sem ninguem ter contratado isso.
 *
 * <h2>Dia inconsistente</h2>
 * Marcacao impar num dia que ja passou significa que faltou registrar uma
 * saida. O numero desse dia e' fantasia da paridade, entao os derivados
 * saem ZERADOS: previsto, excedente e antecipacao valem 0. Os minutos dos
 * pares que fecharam ficam gravados como evidencia para quem for
 * corrigir, e as consultas agregadas ainda excluem o status.
 */
public final class CalculoPermanencia {

    /** A escola vive num fuso so'. Dia civil e horario contratado sao locais. */
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private CalculoPermanencia() {
    }

    /**
     * Um intervalo dentro do dia. {@code saida} nula = par aberto: o aluno
     * ainda esta na escola e o intervalo NAO soma minutos, porque a
     * permanencia so' encerra em saida efetiva.
     */
    public record Intervalo(Instant entrada, Instant saida, UUID eventoEntradaId, UUID eventoSaidaId) {

        public boolean fechado() {
            return saida != null;
        }

        public int minutos() {
            if (saida == null) {
                return 0;
            }
            long m = Duration.between(entrada, saida).toMinutes();
            // Relogio de leitor pode voltar atras numa sincronizacao de NTP;
            // minuto negativo viraria desconto de permanencia de outro par.
            return m < 0 ? 0 : (int) m;
        }
    }

    /** O contratado para AQUELE dia, ja com a excecao pontual aplicada. */
    public record ParametrosDia(boolean diaLetivo,
                                boolean frequenta,
                                LocalTime entradaPrevista,
                                LocalTime saidaPrevista,
                                int cargaMinutos,
                                int toleranciaEntradaMin,
                                int toleranciaSaidaMin,
                                RegraExcedente regraExcedente) {

        public static ParametrosDia semContrato() {
            return new ParametrosDia(true, false, null, null, 0, 0, 0, RegraExcedente.HORARIO);
        }
    }

    public record Totais(Instant primeiraEntrada,
                         Instant ultimaSaida,
                         int minutosPermanencia,
                         int minutosPrevistos,
                         int minutosExcedente,
                         int minutosAntecipacao,
                         int minutosAtrasoEntrada) {
    }

    public record ResultadoDia(StatusPresenca status, List<Intervalo> intervalos, Totais totais) {
    }

    /**
     * Pareia os eventos por posicao. Numero impar deixa o ultimo par
     * aberto (saida nula) — e' ele que vira ABERTA hoje ou INCONSISTENTE
     * depois.
     */
    public static List<Intervalo> parear(List<EventoAcesso> eventos) {
        List<Intervalo> pares = new ArrayList<>();
        if (eventos == null || eventos.isEmpty()) {
            return pares;
        }
        for (int i = 0; i < eventos.size(); i += 2) {
            EventoAcesso entrada = eventos.get(i);
            EventoAcesso saida = (i + 1 < eventos.size()) ? eventos.get(i + 1) : null;
            pares.add(new Intervalo(
                    entrada.dataHora(),
                    saida == null ? null : saida.dataHora(),
                    entrada.id(),
                    saida == null ? null : saida.id()));
        }
        return pares;
    }

    /**
     * Status do dia a partir dos pares.
     *
     * Par aberto e dia ainda nao encerrado = o aluno esta na escola agora
     * (ABERTA). Par aberto num dia que ja passou = faltou registrar a
     * saida (INCONSISTENTE). Sem par nenhum o dia e' FECHADO com zero: nao
     * ha nada pendente nem nada a cobrar.
     */
    public static StatusPresenca statusDe(List<Intervalo> pares, LocalDate dia, LocalDate hoje) {
        boolean algumAberto = pares.stream().anyMatch(p -> !p.fechado());
        if (!algumAberto) {
            return StatusPresenca.FECHADA;
        }
        // Dia futuro entra aqui junto com hoje de proposito: um evento com
        // data adiantada (relogio do leitor errado) nao deve virar
        // pendencia de correcao antes da hora.
        return dia.isBefore(hoje) ? StatusPresenca.INCONSISTENTE : StatusPresenca.ABERTA;
    }

    /**
     * Confronta o realizado com o contratado.
     *
     * @param status status ja decidido para o dia — INCONSISTENTE zera os
     *               derivados, porque nenhum deles e' confiavel sem a
     *               saida que faltou.
     */
    public static Totais totalizar(List<Intervalo> pares, ParametrosDia p, StatusPresenca status, LocalDate dia) {
        Instant primeiraEntrada = pares.isEmpty() ? null : pares.get(0).entrada();
        Instant ultimaSaida = null;
        int permanencia = 0;
        for (Intervalo par : pares) {
            if (par.fechado()) {
                permanencia += par.minutos();
                ultimaSaida = par.saida();
            }
        }

        if (status == StatusPresenca.INCONSISTENTE) {
            return new Totais(primeiraEntrada, ultimaSaida, permanencia, 0, 0, 0, 0);
        }

        int previsto = (p.diaLetivo() && p.frequenta()) ? Math.max(0, p.cargaMinutos()) : 0;

        int excedenteDuracao = Math.max(0, permanencia - previsto - p.toleranciaSaidaMin());

        int excedenteHorario = 0;
        int antecipacao = 0;
        if (ultimaSaida != null && p.saidaPrevista() != null) {
            long desvio = minutosEntre(dia, p.saidaPrevista(), ultimaSaida);
            excedenteHorario = (int) Math.max(0, desvio - p.toleranciaSaidaMin());
            antecipacao = (int) Math.max(0, -desvio - p.toleranciaSaidaMin());
        }

        int excedente = switch (p.regraExcedente()) {
            case DURACAO -> excedenteDuracao;
            // Sem saida prevista no contrato a regra HORARIO nao tem
            // referencia: fica zero em vez de inventar um horario padrao.
            case HORARIO -> excedenteHorario;
            case AMBOS -> Math.max(excedenteDuracao, excedenteHorario);
        };

        int atrasoEntrada = 0;
        if (primeiraEntrada != null && p.entradaPrevista() != null) {
            long desvio = minutosEntre(dia, p.entradaPrevista(), primeiraEntrada);
            atrasoEntrada = (int) Math.max(0, desvio - p.toleranciaEntradaMin());
        }

        return new Totais(primeiraEntrada, ultimaSaida, permanencia, previsto, excedente, antecipacao, atrasoEntrada);
    }

    /** Pareia, classifica e totaliza um dia inteiro a partir dos eventos. */
    public static ResultadoDia apurar(List<EventoAcesso> eventos, ParametrosDia p, LocalDate dia, LocalDate hoje) {
        List<Intervalo> pares = parear(eventos);
        StatusPresenca status = statusDe(pares, dia, hoje);
        return new ResultadoDia(status, pares, totalizar(pares, p, status, dia));
    }

    /** Minutos que {@code momento} passou do horario previsto naquele dia. Negativo = antes. */
    private static long minutosEntre(LocalDate dia, LocalTime previsto, Instant momento) {
        Instant referencia = dia.atTime(previsto).atZone(ZONE).toInstant();
        return Duration.between(referencia, momento).toMinutes();
    }
}
