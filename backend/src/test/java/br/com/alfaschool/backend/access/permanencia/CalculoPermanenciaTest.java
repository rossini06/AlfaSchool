package br.com.alfaschool.backend.access.permanencia;

import br.com.alfaschool.backend.application.access.permanencia.CalculoPermanencia;
import br.com.alfaschool.backend.application.access.permanencia.CalculoPermanencia.Intervalo;
import br.com.alfaschool.backend.application.access.permanencia.CalculoPermanencia.ParametrosDia;
import br.com.alfaschool.backend.application.access.permanencia.CalculoPermanencia.ResultadoDia;
import br.com.alfaschool.backend.application.access.permanencia.CalculoPermanencia.Totais;
import br.com.alfaschool.backend.application.access.permanencia.EventoAcessoLeitor.EventoAcesso;
import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aritmetica da apuracao. Este e' o teste que protege a fatura: cada caso
 * aqui corresponde a uma conta que a escola confere a mao.
 */
class CalculoPermanenciaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);   // terca-feira
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 10);
    private static final LocalDate AMANHA = LocalDate.of(2026, 3, 11);

    // ------------------------------------------------------------ pareamento

    @Test
    void semEventoNaoHaPar() {
        ResultadoDia r = CalculoPermanencia.apurar(List.of(), contrato(RegraExcedente.DURACAO, 0), DIA, HOJE);

        assertThat(r.intervalos()).isEmpty();
        assertThat(r.status()).isEqualTo(StatusPresenca.FECHADA);
        assertThat(r.totais().minutosPermanencia()).isZero();
    }

    @Test
    void doisEventosFormamUmParFechado() {
        ResultadoDia r = CalculoPermanencia.apurar(
                List.of(evento(DIA, 8, 0), evento(DIA, 12, 0)), contrato(RegraExcedente.DURACAO, 0), DIA, HOJE);

        assertThat(r.intervalos()).hasSize(1);
        assertThat(r.intervalos().get(0).fechado()).isTrue();
        assertThat(r.status()).isEqualTo(StatusPresenca.FECHADA);
        assertThat(r.totais().minutosPermanencia()).isEqualTo(240);
    }

    @Test
    void quatroEventosFormamDoisParesQueSomam() {
        ResultadoDia r = CalculoPermanencia.apurar(
                List.of(evento(DIA, 8, 0), evento(DIA, 12, 0), evento(DIA, 13, 0), evento(DIA, 17, 0)),
                contrato(RegraExcedente.DURACAO, 0), DIA, HOJE);

        assertThat(r.intervalos()).hasSize(2);
        assertThat(r.status()).isEqualTo(StatusPresenca.FECHADA);
        // 4h de manha + 4h a' tarde: a saida para a consulta nao conta.
        assertThat(r.totais().minutosPermanencia()).isEqualTo(480);
        assertThat(r.totais().primeiraEntrada()).isEqualTo(em(DIA, 8, 0));
        assertThat(r.totais().ultimaSaida()).isEqualTo(em(DIA, 17, 0));
    }

    @Test
    void tresEventosNoDiaCorrenteDeixamODiaAberto() {
        ResultadoDia r = CalculoPermanencia.apurar(
                List.of(evento(DIA, 8, 0), evento(DIA, 12, 0), evento(DIA, 13, 0)),
                contrato(RegraExcedente.DURACAO, 0), DIA, HOJE);

        assertThat(r.status()).isEqualTo(StatusPresenca.ABERTA);
        assertThat(r.intervalos()).hasSize(2);
        assertThat(r.intervalos().get(1).fechado()).isFalse();
        // Par aberto nao soma: so' os 240 min do par que fechou.
        assertThat(r.totais().minutosPermanencia()).isEqualTo(240);
    }

    @Test
    void tresEventosEmDiaQueJaPassouViramInconsistente() {
        ResultadoDia r = CalculoPermanencia.apurar(
                List.of(evento(DIA, 8, 0), evento(DIA, 12, 0), evento(DIA, 13, 0)),
                contrato(RegraExcedente.DURACAO, 0), DIA, AMANHA);

        assertThat(r.status()).isEqualTo(StatusPresenca.INCONSISTENTE);
    }

    // ------------------------------------------- dia inconsistente nao conta

    @Test
    void diaInconsistenteZeraTodosOsDerivados() {
        ResultadoDia r = CalculoPermanencia.apurar(
                List.of(evento(DIA, 8, 0), evento(DIA, 12, 0), evento(DIA, 13, 0)),
                contrato(RegraExcedente.AMBOS, 0), DIA, AMANHA);

        Totais t = r.totais();
        assertThat(r.status()).isEqualTo(StatusPresenca.INCONSISTENTE);
        assertThat(t.minutosPrevistos()).isZero();
        assertThat(t.minutosExcedente()).isZero();
        assertThat(t.minutosAntecipacao()).isZero();
        // Os minutos que fecharam ficam como evidencia para quem corrigir.
        assertThat(t.minutosPermanencia()).isEqualTo(240);
    }

    // --------------------------------------------------- regras de excedente

    /**
     * O caso que obriga a escola a escolher: contratado 07h-17h, realizado
     * 08h-18h. Permaneceu exatamente o contratado, mas saiu uma hora
     * depois do combinado.
     */
    @Test
    void contratado07as17RealizadoDas08as18NaoTemExcedentePorDuracao() {
        Totais t = totalizar(RegraExcedente.DURACAO, 0);

        assertThat(t.minutosPermanencia()).isEqualTo(600);
        assertThat(t.minutosPrevistos()).isEqualTo(600);
        assertThat(t.minutosExcedente()).isZero();
    }

    @Test
    void contratado07as17RealizadoDas08as18TemUmaHoraDeExcedentePorHorario() {
        Totais t = totalizar(RegraExcedente.HORARIO, 0);

        assertThat(t.minutosExcedente()).isEqualTo(60);
    }

    @Test
    void regraAmbosPegaOMaiorDosDois() {
        Totais t = totalizar(RegraExcedente.AMBOS, 0);

        assertThat(t.minutosExcedente()).isEqualTo(60);
    }

    @Test
    void duracaoCobraQuandoOTotalDeHorasPassaDoContratado() {
        // 07h30 as 18h = 630 min contra 600 contratados.
        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 7, 30), em(DIA, 18, 0), null, null)),
                contrato(RegraExcedente.DURACAO, 0), StatusPresenca.FECHADA, DIA);

        assertThat(t.minutosExcedente()).isEqualTo(30);
    }

    // ------------------------------------------------------------ tolerancia

    @Test
    void toleranciaZeraOExcedenteNaBordaExata() {
        // Saiu 60 min depois com 60 min de franquia: nada a cobrar.
        Totais t = totalizar(RegraExcedente.HORARIO, 60);

        assertThat(t.minutosExcedente()).isZero();
    }

    @Test
    void umMinutoAlemDaToleranciaJaEExcedente() {
        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 8, 0), em(DIA, 18, 1), null, null)),
                contrato(RegraExcedente.HORARIO, 60), StatusPresenca.FECHADA, DIA);

        assertThat(t.minutosExcedente()).isEqualTo(1);
    }

    @Test
    void toleranciaDeEntradaNaoInterfereNoExcedente() {
        ParametrosDia p = new ParametrosDia(true, true, LocalTime.of(7, 0), LocalTime.of(17, 0),
                600, 120, 0, RegraExcedente.HORARIO);

        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 8, 0), em(DIA, 18, 0), null, null)),
                p, StatusPresenca.FECHADA, DIA);

        // A franquia de 120 min de entrada nao pode virar franquia de saida.
        assertThat(t.minutosExcedente()).isEqualTo(60);
        // Chegou 60 min depois com 120 de franquia: sem atraso.
        assertThat(t.minutosAtrasoEntrada()).isZero();
    }

    @Test
    void saidaAntesDoContratadoGeraAntecipacao() {
        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 7, 0), em(DIA, 16, 0), null, null)),
                contrato(RegraExcedente.HORARIO, 0), StatusPresenca.FECHADA, DIA);

        assertThat(t.minutosAntecipacao()).isEqualTo(60);
        assertThat(t.minutosExcedente()).isZero();
    }

    // ------------------------------------------------------------- previsto 0

    @Test
    void jornadaComFrequentaFalsoTemPrevistoZero() {
        ParametrosDia p = new ParametrosDia(true, false, LocalTime.of(7, 0), LocalTime.of(17, 0),
                600, 0, 0, RegraExcedente.DURACAO);

        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 8, 0), em(DIA, 11, 0), null, null)),
                p, StatusPresenca.FECHADA, DIA);

        assertThat(t.minutosPrevistos()).isZero();
        // Sem nada contratado naquele dia, a permanencia inteira e' excedente.
        assertThat(t.minutosExcedente()).isEqualTo(180);
    }

    @Test
    void diaNaoLetivoTemPrevistoZero() {
        ParametrosDia p = new ParametrosDia(false, true, LocalTime.of(7, 0), LocalTime.of(17, 0),
                600, 0, 0, RegraExcedente.DURACAO);

        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 8, 0), em(DIA, 11, 0), null, null)),
                p, StatusPresenca.FECHADA, DIA);

        assertThat(t.minutosPrevistos()).isZero();
    }

    @Test
    void contratoSemSaidaPrevistaNaoCobraPorHorario() {
        ParametrosDia p = new ParametrosDia(true, true, null, null, 600, 0, 0, RegraExcedente.HORARIO);

        Totais t = CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 7, 0), em(DIA, 19, 0), null, null)),
                p, StatusPresenca.FECHADA, DIA);

        assertThat(t.minutosExcedente()).isZero();
    }

    // ---------------------------------------------------------------- helpers

    /** Contratado 07h-17h (600 min), realizado 08h-18h. */
    private static Totais totalizar(RegraExcedente regra, int toleranciaSaida) {
        return CalculoPermanencia.totalizar(
                List.of(new Intervalo(em(DIA, 8, 0), em(DIA, 18, 0), null, null)),
                contrato(regra, toleranciaSaida), StatusPresenca.FECHADA, DIA);
    }

    // =================================================================
    // Repique de leitura
    //
    // Encontrado pelo smoke, nao pelos testes unitarios: um dia real de
    // 3h59 apurou ZERO minuto, com status FECHADA — ou seja, entrou nos
    // totais como se a crianca nao tivesse ficado na escola.
    // =================================================================

    @Test
    @DisplayName("Crachao encostado duas vezes na entrada nao pode zerar o dia")
    void repiqueNaEntradaNaoZeraODia() {
        // E, E(+14s), S, S(+14s) — o que o equipamento grava quando a
        // crianca nao ve a luz verde e encosta de novo.
        List<EventoAcesso> eventos = List.of(
                eventoEm(DIA, 7, 46, 39),
                eventoEm(DIA, 7, 46, 53),
                eventoEm(DIA, 11, 45, 39),
                eventoEm(DIA, 11, 45, 53));

        ResultadoDia r = CalculoPermanencia.apurar(eventos, contrato(RegraExcedente.HORARIO, 0), DIA, DIA.plusDays(1));

        assertThat(r.totais().minutosPermanencia()).isEqualTo(239);
        assertThat(r.status()).isEqualTo(StatusPresenca.FECHADA);
        assertThat(r.intervalos()).hasSize(1);
    }

    @Test
    @DisplayName("O horario que vale e' o da PRIMEIRA leitura, nao o da repetida")
    void repiqueMantemOHorarioDaPrimeiraLeitura() {
        List<EventoAcesso> limpos = CalculoPermanencia.semRepique(List.of(
                eventoEm(DIA, 7, 0, 0),
                eventoEm(DIA, 7, 0, 30),
                eventoEm(DIA, 17, 0, 0)));

        assertThat(limpos).hasSize(2);
        assertThat(limpos.get(0).dataHora()).isEqualTo(em(DIA, 7, 0));
    }

    @Test
    @DisplayName("Saida e entrada de verdade, fora da janela, continuam sendo dois eventos")
    void passagensRealmenteDistintasSobrevivem() {
        // Almoco em casa: sai 11h30, volta 13h. Nada a ver com repique.
        List<EventoAcesso> eventos = List.of(
                evento(DIA, 7, 0), evento(DIA, 11, 30),
                evento(DIA, 13, 0), evento(DIA, 17, 0));

        ResultadoDia r = CalculoPermanencia.apurar(eventos, contrato(RegraExcedente.HORARIO, 0), DIA, DIA.plusDays(1));

        assertThat(r.intervalos()).hasSize(2);
        assertThat(r.totais().minutosPermanencia()).isEqualTo(270 + 240);
    }

    @Test
    @DisplayName("Dia inconsistente continua inconsistente: o antirepique nao inventa saida")
    void antirepiqueNaoConsertaDiaSemSaida() {
        List<EventoAcesso> eventos = List.of(eventoEm(DIA, 7, 0, 0), eventoEm(DIA, 7, 0, 20));

        ResultadoDia r = CalculoPermanencia.apurar(eventos, contrato(RegraExcedente.HORARIO, 0), DIA, DIA.plusDays(1));

        assertThat(r.status()).isEqualTo(StatusPresenca.INCONSISTENTE);
        assertThat(r.totais().minutosPermanencia()).isZero();
    }

    private static ParametrosDia contrato(RegraExcedente regra, int toleranciaSaida) {
        return new ParametrosDia(true, true, LocalTime.of(7, 0), LocalTime.of(17, 0),
                600, 0, toleranciaSaida, regra);
    }

    private static EventoAcesso evento(LocalDate dia, int hora, int minuto) {
        return new EventoAcesso(UUID.randomUUID(), em(dia, hora, minuto), null, SentidoAcesso.INDEFINIDO);
    }

    private static EventoAcesso eventoEm(LocalDate dia, int hora, int minuto, int segundo) {
        return new EventoAcesso(UUID.randomUUID(),
                dia.atTime(hora, minuto, segundo).atZone(CalculoPermanencia.ZONE).toInstant(),
                null, SentidoAcesso.INDEFINIDO);
    }

    private static Instant em(LocalDate dia, int hora, int minuto) {
        return dia.atTime(hora, minuto).atZone(CalculoPermanencia.ZONE).toInstant();
    }
}
