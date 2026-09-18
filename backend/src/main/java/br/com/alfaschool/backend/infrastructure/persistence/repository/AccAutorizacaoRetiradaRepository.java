package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccAutorizacaoRetiradaRepository extends JpaRepository<AutorizacaoRetirada, UUID> {

    Optional<AutorizacaoRetirada> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

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
