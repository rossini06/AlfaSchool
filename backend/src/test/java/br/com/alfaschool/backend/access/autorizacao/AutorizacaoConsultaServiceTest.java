package br.com.alfaschool.backend.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.AutorizacaoConsultaService;
import br.com.alfaschool.backend.application.access.shared.AutorizacaoPort.Veredito;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRestricaoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.alfaschool.backend.access.autorizacao.AutorizacaoTestFixtures.autorizacao;
import static br.com.alfaschool.backend.access.autorizacao.AutorizacaoTestFixtures.pessoa;
import static br.com.alfaschool.backend.access.autorizacao.AutorizacaoTestFixtures.restricao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Matriz de verificacao da portaria. Cada caso aqui corresponde a uma forma
 * conhecida de entregar uma crianca a pessoa errada.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutorizacaoConsultaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();
    private static final UUID PESSOA = UUID.randomUUID();
    private static final UUID AUTORIZACAO = UUID.randomUUID();
    private static final String CPF = "52998224725";

    /** Quarta-feira, 12:00 em Sao Paulo (15:00Z). ISO: quarta = 3. */
    private static final Instant QUARTA_MEIO_DIA = Instant.parse("2025-03-05T15:00:00Z");

    @Mock private AccRestricaoRepository restricaoRepository;
    @Mock private AccPessoaAutorizadaRepository pessoaRepository;
    @Mock private AccAutorizacaoRetiradaRepository autorizacaoRepository;
    @Mock private AlunoRepository alunoRepository;

    private AutorizacaoConsultaService service;

    @BeforeEach
    void setUp() {
        service = new AutorizacaoConsultaService(
                restricaoRepository, pessoaRepository, autorizacaoRepository, alunoRepository);
        TenantContext.setTenantId(TENANT);
        semRestricoes();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------
    // 1. Restricao tem precedencia absoluta
    // ------------------------------------------------------------------

    @Test
    @DisplayName("restricao ativa vence autorizacao ATIVA e permanente")
    void restricaoVenceAutorizacaoAtivaPermanente() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada permanenteAtiva = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        permanenteAtiva.setPermanente(true);
        autorizacoesDoPar(permanenteAtiva);

        Restricao judicial = restricao(TENANT, ALUNO, PESSOA, null);
        when(restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(TENANT, ALUNO))
                .thenReturn(List.of(judicial));

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isFalse();
        assertThat(veredito.motivo()).isEqualTo("Restricao judicial vigente");
        assertThat(veredito.autorizacaoId()).isNull();
    }

    @Test
    @DisplayName("restricao por CPF solto, sem pessoa_autorizada_id, tambem bloqueia")
    void restricaoPorCpfSoltoBloqueia() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA));

        // A decisao judicial chegou so com nome e CPF, com a mascara do oficio.
        Restricao porCpf = restricao(TENANT, ALUNO, null, "529.982.247-25");
        when(restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(TENANT, ALUNO))
                .thenReturn(List.of(porCpf));

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isFalse();
        assertThat(veredito.motivo()).isEqualTo("Restricao judicial vigente");
    }

    @Test
    @DisplayName("restricao ja encerrada por vigencia nao bloqueia")
    void restricaoForaDaVigenciaNaoBloqueia() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA));

        Restricao vencida = restricao(TENANT, ALUNO, PESSOA, null);
        vencida.setVigenciaFim(LocalDate.of(2025, 3, 4));
        when(restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(TENANT, ALUNO))
                .thenReturn(List.of(vencida));

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isTrue();
    }

    // ------------------------------------------------------------------
    // 2. Permissoes independentes da pessoa
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pessoa com pode_retirar=false e negada mesmo com autorizacao ATIVA")
    void podeRetirarFalseNegaMesmoComAutorizacaoAtiva() {
        PessoaAutorizada semPermissao = pessoa(PESSOA, TENANT, CPF, false, true);
        // Portal e notificacao ligados de proposito: nenhuma das duas pode
        // virar permissao de retirada.
        semPermissao.setPodeAcessarPortal(true);
        semPermissao.setRecebeNotificacao(true);
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(PESSOA, TENANT))
                .thenReturn(Optional.of(semPermissao));
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA));

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isFalse();
        assertThat(veredito.motivo()).isEqualTo("Pessoa sem permissao de retirada");
    }

    @Test
    @DisplayName("pessoa inativa e negada")
    void pessoaInativaNegada() {
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(PESSOA, TENANT))
                .thenReturn(Optional.of(pessoa(PESSOA, TENANT, CPF, true, false)));
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA));

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Pessoa autorizada inativa");
    }

    @Test
    @DisplayName("pessoa inexistente ou deletada e negada")
    void pessoaInexistenteNegada() {
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(PESSOA, TENANT)).thenReturn(Optional.empty());

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Pessoa autorizada nao encontrada");
    }

    // ------------------------------------------------------------------
    // 3. Motivo especifico por status
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PENDENTE devolve motivo de aguardando aprovacao")
    void statusPendente() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.PENDENTE));

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Autorizacao aguardando aprovacao da escola");
    }

    @Test
    @DisplayName("SUSPENSA devolve motivo de suspensao")
    void statusSuspensa() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.SUSPENSA));

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Autorizacao suspensa");
    }

    @Test
    @DisplayName("REVOGADA devolve motivo de nao vigente")
    void statusRevogada() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.REVOGADA));

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Autorizacao nao vigente");
    }

    @Test
    @DisplayName("EXPIRADA devolve motivo de nao vigente")
    void statusExpirada() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar(autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.EXPIRADA));

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Autorizacao nao vigente");
    }

    @Test
    @DisplayName("sem autorizacao nenhuma devolve nao autorizada")
    void semAutorizacao() {
        pessoaCadastrada(true, true);
        autorizacoesDoPar();

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Pessoa nao autorizada a retirar este aluno");
    }

    // ------------------------------------------------------------------
    // 4/5/6. Vigencia, dia da semana e faixa de horario
    // ------------------------------------------------------------------

    @Test
    @DisplayName("antes do inicio da vigencia e negada")
    void antesDaVigencia() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setVigenciaInicio(LocalDate.of(2025, 3, 10));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Autorizacao ainda nao vigente");
    }

    @Test
    @DisplayName("depois do fim da vigencia e negada")
    void depoisDaVigencia() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setPermanente(false);
        a.setVigenciaFim(LocalDate.of(2025, 3, 4));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).motivo())
                .isEqualTo("Autorizacao fora do periodo de vigencia");
    }

    @Test
    @DisplayName("fora do dia da semana e negada, com os dias no motivo")
    void foraDoDiaDaSemana() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setDiasSemana("5"); // so sexta; o momento e' quarta
        autorizacoesDoPar(a);

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isFalse();
        assertThat(veredito.motivo()).isEqualTo("Autorizada apenas em sexta");
    }

    @Test
    @DisplayName("dentro do dia da semana e permitida")
    void dentroDoDiaDaSemana() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setDiasSemana("1,3,5");
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isTrue();
    }

    @Test
    @DisplayName("fora da faixa de horario e negada")
    void foraDaFaixaDeHorario() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setHoraInicio(LocalTime.of(17, 0));
        a.setHoraFim(LocalTime.of(18, 0));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isFalse();
    }

    @Test
    @DisplayName("borda: exatamente hora_inicio esta dentro")
    void bordaHoraInicio() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setHoraInicio(LocalTime.of(12, 0));
        a.setHoraFim(LocalTime.of(18, 0));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isTrue();
    }

    @Test
    @DisplayName("borda: exatamente hora_fim esta dentro")
    void bordaHoraFim() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setHoraInicio(LocalTime.of(7, 0));
        a.setHoraFim(LocalTime.of(12, 0));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isTrue();
    }

    @Test
    @DisplayName("um minuto depois de hora_fim ja esta fora")
    void umMinutoDepoisDeHoraFim() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setHoraInicio(LocalTime.of(7, 0));
        a.setHoraFim(LocalTime.of(11, 59));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isFalse();
    }

    // ------------------------------------------------------------------
    // Fuso: o Instant e' UTC, a escola opera em America/Sao_Paulo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("virada de dia: 05/03 02:00Z ainda e' terca 04/03 em Sao Paulo")
    void viradaDeDiaUsaFusoDeSaoPaulo() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setDiasSemana("2"); // terca
        autorizacoesDoPar(a);

        Instant instanteUtc = Instant.parse("2025-03-05T02:00:00Z");

        // Em UTC seria quarta (3) e a retirada seria negada; em Sao Paulo e'
        // terca 23:00, dia autorizado.
        assertThat(service.verificar(ALUNO, PESSOA, instanteUtc).permitido()).isTrue();
    }

    @Test
    @DisplayName("virada de dia: vigencia que termina em 04/03 ainda cobre 05/03 02:00Z")
    void viradaDeDiaNaVigencia() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setPermanente(false);
        a.setVigenciaFim(LocalDate.of(2025, 3, 4));
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, Instant.parse("2025-03-05T02:00:00Z")).permitido()).isTrue();
    }

    @Test
    @DisplayName("virada de dia: quarta em UTC e quarta em Sao Paulo no meio do dia")
    void mesmoDiaNoMeioDoDia() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setDiasSemana("3");
        autorizacoesDoPar(a);

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isTrue();
    }

    // ------------------------------------------------------------------
    // Falha fechada
    // ------------------------------------------------------------------

    @Test
    @DisplayName("excecao interna vira negacao, nunca permissao")
    void excecaoInternaViraNegacao() {
        when(restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(any(), any()))
                .thenThrow(new RuntimeException("banco fora do ar"));
        pessoaCadastrada(true, true);

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isFalse();
        assertThat(veredito.motivo()).isEqualTo("Falha ao validar autorizacao");
        assertThat(veredito.autorizacaoId()).isNull();
    }

    @Test
    @DisplayName("sem tenant no contexto nega em vez de vazar entre escolas")
    void semTenantNega() {
        TenantContext.clear();

        assertThat(service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA).permitido()).isFalse();
    }

    @Test
    @DisplayName("parametros nulos negam")
    void parametrosNulosNegam() {
        assertThat(service.verificar(null, PESSOA, QUARTA_MEIO_DIA).permitido()).isFalse();
        assertThat(service.verificar(ALUNO, null, QUARTA_MEIO_DIA).permitido()).isFalse();
        assertThat(service.verificar(ALUNO, PESSOA, null).permitido()).isFalse();
    }

    // ------------------------------------------------------------------
    // Caso feliz e devolucao do id
    // ------------------------------------------------------------------

    @Test
    @DisplayName("tudo em ordem permite e devolve a autorizacao aplicada")
    void casoFeliz() {
        pessoaCadastrada(true, true);
        AutorizacaoRetirada a = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        a.setVigenciaInicio(LocalDate.of(2025, 1, 1));
        a.setDiasSemana("1,2,3,4,5");
        a.setHoraInicio(LocalTime.of(7, 0));
        a.setHoraFim(LocalTime.of(19, 0));
        autorizacoesDoPar(a);

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isTrue();
        assertThat(veredito.motivo()).isNull();
        assertThat(veredito.autorizacaoId()).isEqualTo(AUTORIZACAO);
    }

    @Test
    @DisplayName("com duas ATIVAS, basta uma cobrir o momento")
    void duasAtivasBastaUmaCobrir() {
        pessoaCadastrada(true, true);

        AutorizacaoRetirada soSexta = autorizacao(UUID.randomUUID(), TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        soSexta.setDiasSemana("5");

        AutorizacaoRetirada soQuarta = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        soQuarta.setDiasSemana("3");

        autorizacoesDoPar(soSexta, soQuarta);

        Veredito veredito = service.verificar(ALUNO, PESSOA, QUARTA_MEIO_DIA);

        assertThat(veredito.permitido()).isTrue();
        assertThat(veredito.autorizacaoId()).isEqualTo(AUTORIZACAO);
    }

    // ------------------------------------------------------------------
    // quemPodeRetirarAgora / alunosQuePodeRetirar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("quemPodeRetirarAgora nao lista quem esta sob restricao")
    void quemPodeRetirarAgoraRespeitaRestricao() {
        PessoaAutorizada liberada = pessoa(PESSOA, TENANT, CPF, true, true);
        UUID outraId = UUID.randomUUID();
        PessoaAutorizada restrita = pessoa(outraId, TENANT, "39053344705", true, true);

        AutorizacaoRetirada aLiberada = autorizacao(AUTORIZACAO, TENANT, ALUNO, PESSOA, StatusAutorizacao.ATIVA);
        AutorizacaoRetirada aRestrita = autorizacao(UUID.randomUUID(), TENANT, ALUNO, outraId, StatusAutorizacao.ATIVA);

        when(autorizacaoRepository.findByTenantIdAndAlunoIdAndStatusAndDeletedFalse(
                TENANT, ALUNO, StatusAutorizacao.ATIVA)).thenReturn(List.of(aLiberada, aRestrita));
        when(pessoaRepository.findByTenantIdAndIdInAndDeletedFalse(TENANT, List.of(PESSOA, outraId)))
                .thenReturn(List.of(liberada, restrita));
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(PESSOA, TENANT)).thenReturn(Optional.of(liberada));
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(outraId, TENANT)).thenReturn(Optional.of(restrita));
        when(autorizacaoRepository.findByTenantIdAndAlunoIdAndPessoaAutorizadaIdAndDeletedFalse(TENANT, ALUNO, PESSOA))
                .thenReturn(List.of(aLiberada));
        when(autorizacaoRepository.findByTenantIdAndAlunoIdAndPessoaAutorizadaIdAndDeletedFalse(TENANT, ALUNO, outraId))
                .thenReturn(List.of(aRestrita));
        when(restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(TENANT, ALUNO))
                .thenReturn(List.of(restricao(TENANT, ALUNO, outraId, null)));

        assertThat(service.quemPodeRetirarAgora(ALUNO, QUARTA_MEIO_DIA))
                .extracting("id")
                .containsExactly(PESSOA);
    }

    // ------------------------------------------------------------------
    // Ajudantes
    // ------------------------------------------------------------------

    private void pessoaCadastrada(boolean podeRetirar, boolean ativo) {
        when(pessoaRepository.findByIdAndTenantIdAndDeletedFalse(PESSOA, TENANT))
                .thenReturn(Optional.of(pessoa(PESSOA, TENANT, CPF, podeRetirar, ativo)));
    }

    private void autorizacoesDoPar(AutorizacaoRetirada... autorizacoes) {
        when(autorizacaoRepository.findByTenantIdAndAlunoIdAndPessoaAutorizadaIdAndDeletedFalse(TENANT, ALUNO, PESSOA))
                .thenReturn(List.of(autorizacoes));
    }

    private void semRestricoes() {
        when(restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(TENANT, ALUNO))
                .thenReturn(List.of());
    }
}
