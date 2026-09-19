package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccAutorizacaoRetiradaRepository extends JpaRepository<AutorizacaoRetirada, UUID> {

    Optional<AutorizacaoRetirada> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * Listagem da tela, que nao existia: havia apenas busca por aluno ou por
     * pessoa, entao GET /access/autorizacoes respondia 405 e tanto a tabela
     * principal quanto a fila de aprovacao do portal ficavam vazias.
     */
    @Query("select a from AutorizacaoRetirada a where a.tenantId = :tenantId and a.deleted = false "
         + "and (:alunoId is null or a.alunoId = :alunoId) "
         + "and (:status is null or a.status = :status) "
         + "order by a.createdAt desc")
    Page<AutorizacaoRetirada> buscar(@Param("tenantId") UUID tenantId,
                                     @Param("alunoId") UUID alunoId,
                                     @Param("status") StatusAutorizacao status,
                                     Pageable pageable);

    List<AutorizacaoRetirada> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    List<AutorizacaoRetirada> findByTenantIdAndPessoaAutorizadaIdAndDeletedFalse(UUID tenantId, UUID pessoaAutorizadaId);

    /**
     * Usado pela verificacao da portaria. Devolve TODAS as autorizacoes do par
     * (nao so as ATIVAS) porque o motivo da negativa depende do status
     * encontrado — "aguardando aprovacao" e "suspensa" precisam ser distintos
     * de "nao autorizada".
     */
    List<AutorizacaoRetirada> findByTenantIdAndAlunoIdAndPessoaAutorizadaIdAndDeletedFalse(
            UUID tenantId, UUID alunoId, UUID pessoaAutorizadaId);

    List<AutorizacaoRetirada> findByTenantIdAndAlunoIdAndStatusAndDeletedFalse(
            UUID tenantId, UUID alunoId, StatusAutorizacao status);

    List<AutorizacaoRetirada> findByTenantIdAndPessoaAutorizadaIdAndStatusAndDeletedFalse(
            UUID tenantId, UUID pessoaAutorizadaId, StatusAutorizacao status);

    /**
     * Job de expiracao: roda fora de requisicao, sem TenantContext, entao
     * varre todos os tenants de proposito.
     */
    List<AutorizacaoRetirada> findByStatusAndVigenciaFimBeforeAndDeletedFalse(
            StatusAutorizacao status, LocalDate limite);
}
