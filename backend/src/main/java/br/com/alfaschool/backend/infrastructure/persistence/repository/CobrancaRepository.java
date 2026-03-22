package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.financeiro.Cobranca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, UUID> {

    Page<Cobranca> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<Cobranca> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    List<Cobranca> findByTenantIdAndContratoIdAndDeletedFalse(UUID tenantId, UUID contratoId);

    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, String status);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.status = 'pendente' AND c.vencimento < :hoje")
    BigDecimal sumInadimplencia(UUID tenantId, LocalDate hoje);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.status = 'pago' AND c.dataPagamento >= :inicio AND c.dataPagamento <= :fim")
    BigDecimal sumReceitaRecebida(UUID tenantId, LocalDate inicio, LocalDate fim);

    @Query("SELECT COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.vencimento >= :inicio AND c.vencimento <= :fim")
    BigDecimal sumReceitaPrevista(UUID tenantId, LocalDate inicio, LocalDate fim);

    boolean existsByTenantIdAndContratoIdAndCompetenciaAndDeletedFalse(UUID tenantId, UUID contratoId, String competencia);
}
