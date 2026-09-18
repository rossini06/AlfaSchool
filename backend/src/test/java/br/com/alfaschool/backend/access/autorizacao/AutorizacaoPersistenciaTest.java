package br.com.alfaschool.backend.access.autorizacao;

import br.com.alfaschool.backend.domain.access.autorizacao.AcaoAutorizacao;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRestricaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Fatia JPA isolada. Existe para que todo mapeamento @Entity e toda derived
 * query destes repositorios sejam validados em tempo de build: um nome de
 * metodo errado aqui so apareceria em producao, na hora de liberar uma
 * crianca.
 *
 * ATENCAO: no perfil de teste o schema H2 vem das ENTIDADES
 * (ddl-auto=create-drop), nao da V35. Logo este teste NAO prova que a
 * entidade bate com a migration — nome de tabela, nome de coluna e
 * nulabilidade continuam sendo conferidos a mao contra
 * V35__access_autorizacoes.sql.
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AutorizacaoPersistenciaTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();

    @Autowired private AccPessoaAutorizadaRepository pessoaRepository;
    @Autowired private AccAutorizacaoRetiradaRepository autorizacaoRepository;
    @Autowired private AccAutorizacaoHistoricoRepository historicoRepository;
    @Autowired private AccRestricaoRepository restricaoRepository;

    @Test
    @DisplayName("entidades e derived queries do modulo resolvem contra o banco")
    void mapeamentoEConsultas() {
        PessoaAutorizada pessoa = new PessoaAutorizada();
        pessoa.setTenantId(TENANT);
        pessoa.setNome("Avo Rita");
        pessoa.setCpf("52998224725");
        pessoa.setPodeRetirar(true);
        pessoa = pessoaRepository.save(pessoa);

        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        autorizacao.setTenantId(TENANT);
        autorizacao.setAlunoId(ALUNO);
        autorizacao.setPessoaAutorizadaId(pessoa.getId());
        autorizacao.setStatus(StatusAutorizacao.ATIVA);
        autorizacao.setPermanente(false);
        autorizacao.setVigenciaFim(LocalDate.now().minusDays(1));
        autorizacao.setDiasSemana("1,3,5");
        autorizacao.setHoraInicio(LocalTime.of(7, 0));
        autorizacao.setHoraFim(LocalTime.of(19, 0));
        autorizacao.setObservacao("Busca as sextas");
        autorizacao = autorizacaoRepository.save(autorizacao);

        historicoRepository.save(new AutorizacaoHistorico(
                TENANT, autorizacao.getId(), AcaoAutorizacao.CRIACAO, null,
                StatusAutorizacao.ATIVA, UUID.randomUUID(), "cadastro inicial", "10.0.0.1"));

        Restricao restricao = new Restricao();
        restricao.setTenantId(TENANT);
        restricao.setAlunoId(ALUNO);
        restricao.setPessoaCpf("39053344705");
        restricao.setDescricao("Medida protetiva");
        restricao.setAtivo(true);
        restricaoRepository.save(restricao);

        org.assertj.core.api.Assertions.assertThat(
                pessoaRepository.findByIdAndTenantIdAndDeletedFalse(pessoa.getId(), TENANT)).isPresent();
        org.assertj.core.api.Assertions.assertThat(
                pessoaRepository.existsByTenantIdAndCpfAndDeletedFalse(TENANT, "52998224725")).isTrue();
        org.assertj.core.api.Assertions.assertThat(
                pessoaRepository.findByTenantIdAndIdInAndDeletedFalse(TENANT, List.of(pessoa.getId()))).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(
                pessoaRepository.findByTenantIdAndResponsavelIdAndDeletedFalse(TENANT, UUID.randomUUID())).isEmpty();

        org.assertj.core.api.Assertions.assertThat(
                autorizacaoRepository.findByTenantIdAndAlunoIdAndPessoaAutorizadaIdAndDeletedFalse(
                        TENANT, ALUNO, pessoa.getId())).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(
                autorizacaoRepository.findByTenantIdAndAlunoIdAndStatusAndDeletedFalse(
                        TENANT, ALUNO, StatusAutorizacao.ATIVA)).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(
                autorizacaoRepository.findByTenantIdAndPessoaAutorizadaIdAndStatusAndDeletedFalse(
                        TENANT, pessoa.getId(), StatusAutorizacao.ATIVA)).hasSize(1);
        // Consulta do job de expiracao: varre todos os tenants de proposito.
        org.assertj.core.api.Assertions.assertThat(
                autorizacaoRepository.findByStatusAndVigenciaFimBeforeAndDeletedFalse(
                        StatusAutorizacao.ATIVA, LocalDate.now())).hasSize(1);

        org.assertj.core.api.Assertions.assertThat(
                historicoRepository.findByTenantIdAndAutorizacaoIdOrderByCreatedAtAsc(TENANT, autorizacao.getId()))
                .hasSize(1);

        org.assertj.core.api.Assertions.assertThat(
                restricaoRepository.findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(TENANT, ALUNO)).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(
                restricaoRepository.findByTenantIdAndAlunoIdAndDeletedFalse(TENANT, ALUNO)).hasSize(1);
    }
}
