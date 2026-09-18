package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoPreferencia;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Preferencias/opt-in e a resolucao de quem pode ser avisado.
 *
 * <p>As consultas a {@code responsaveis}, {@code aluno_responsaveis},
 * {@code acc_pessoas_autorizadas} e {@code acc_autorizacoes_retirada} sao
 * NATIVAS de proposito: essas tabelas pertencem a outras fatias e nao devem
 * ganhar uma segunda entidade JPA aqui. As projections de interface entregam
 * so' o que o motor precisa.
 */
@Repository
public interface AccNotificacaoPreferenciaRepository extends JpaRepository<AccNotificacaoPreferencia, UUID> {

    List<AccNotificacaoPreferencia> findByTenantIdAndTitularIdAndDeletedFalse(UUID tenantId, UUID titularId);

    List<AccNotificacaoPreferencia> findByTenantIdAndTitularTipoAndTitularIdAndDeletedFalse(
            UUID tenantId, TitularTipo titularTipo, UUID titularId);

    Optional<AccNotificacaoPreferencia> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccNotificacaoPreferencia> findFirstByTenantIdAndTitularTipoAndTitularIdAndCanalAndEventoIsNullAndDeletedFalse(
            UUID tenantId, TitularTipo titularTipo, UUID titularId, CanalNotificacao canal);

    /** Destinatario resolvido a partir das tabelas de outras fatias. */
    interface DestinatarioProjection {
        String getTitularId();

        String getNome();
    }

    /**
     * Responsaveis vinculados ao aluno.
     *
     * <p>{@code aluno_responsaveis} nao tem coluna {@code recebe_notificacao}
     * (V20), diferente de {@code acc_pessoas_autorizadas}. Nao filtramos aqui e
     * isso e' seguro: o portao real e' o opt-in. Sem linha de preferencia com
     * {@code habilitado = true} e {@code opt_in_em} preenchido, o motor nao cria
     * envio nenhum.
     */
    @Query(value = """
            SELECT CAST(r.id AS CHAR(36)) AS titularId,
                   r.nome                 AS nome
            FROM aluno_responsaveis ar
            JOIN responsaveis r ON r.id = ar.responsavel_id
            WHERE ar.tenant_id = :tenantId
              AND ar.aluno_id = :alunoId
              AND ar.deleted = FALSE
              AND r.deleted = FALSE
            """, nativeQuery = true)
    List<DestinatarioProjection> buscarResponsaveisDoAluno(@Param("tenantId") String tenantId,
                                                           @Param("alunoId") String alunoId);

    /**
     * Pessoas autorizadas do aluno que pediram para ser avisadas.
     *
     * <p>Aqui SIM filtramos por {@code recebe_notificacao}: a V35 deixa claro
     * que retirar, acessar o portal e receber aviso sao permissoes
     * independentes. Quem pode buscar a crianca nao passa a receber a rotina
     * dela por consequencia.
     */
    @Query(value = """
            SELECT DISTINCT CAST(pa.id AS CHAR(36)) AS titularId,
                            pa.nome                 AS nome
            FROM acc_autorizacoes_retirada aut
            JOIN acc_pessoas_autorizadas pa ON pa.id = aut.pessoa_autorizada_id
            WHERE aut.tenant_id = :tenantId
              AND aut.aluno_id = :alunoId
              AND aut.status = 'ATIVA'
              AND aut.deleted = FALSE
              AND pa.recebe_notificacao = TRUE
              AND pa.ativo = TRUE
              AND pa.deleted = FALSE
            """, nativeQuery = true)
    List<DestinatarioProjection> buscarPessoasAutorizadasDoAluno(@Param("tenantId") String tenantId,
                                                                 @Param("alunoId") String alunoId);

    /** Nome de um responsavel (titular direto do evento). */
    @Query(value = """
            SELECT CAST(r.id AS CHAR(36)) AS titularId,
                   r.nome                 AS nome
            FROM responsaveis r
            WHERE r.tenant_id = :tenantId
              AND r.id = :responsavelId
              AND r.deleted = FALSE
            """, nativeQuery = true)
    Optional<DestinatarioProjection> buscarResponsavel(@Param("tenantId") String tenantId,
                                                       @Param("responsavelId") String responsavelId);

    /**
     * Alunos ligados a um responsavel. Usado quando um responsavel e' barrado
     * na portaria e a familia de cada crianca vinculada precisa ser avisada.
     */
    @Query(value = """
            SELECT DISTINCT CAST(ar.aluno_id AS CHAR(36))
            FROM aluno_responsaveis ar
            WHERE ar.tenant_id = :tenantId
              AND ar.responsavel_id = :responsavelId
              AND ar.deleted = FALSE
            """, nativeQuery = true)
    List<String> buscarAlunosDoResponsavel(@Param("tenantId") String tenantId,
                                           @Param("responsavelId") String responsavelId);

    /** Alunos que uma pessoa autorizada pode retirar. */
    @Query(value = """
            SELECT DISTINCT CAST(aut.aluno_id AS CHAR(36))
            FROM acc_autorizacoes_retirada aut
            WHERE aut.tenant_id = :tenantId
              AND aut.pessoa_autorizada_id = :pessoaId
              AND aut.deleted = FALSE
            """, nativeQuery = true)
    List<String> buscarAlunosDaPessoaAutorizada(@Param("tenantId") String tenantId,
                                                @Param("pessoaId") String pessoaId);

    /** Nome do aluno, para compor o texto do aviso. */
    @Query(value = """
            SELECT a.nome
            FROM alunos a
            WHERE a.tenant_id = :tenantId AND a.id = :alunoId AND a.deleted = FALSE
            """, nativeQuery = true)
    Optional<String> buscarNomeDoAluno(@Param("tenantId") String tenantId, @Param("alunoId") String alunoId);
}
