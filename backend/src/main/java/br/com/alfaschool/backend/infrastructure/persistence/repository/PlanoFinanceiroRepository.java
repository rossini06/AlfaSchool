package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.financeiro.PlanoFinanceiro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanoFinanceiroRepository extends JpaRepository<PlanoFinanceiro, UUID> {

    Page<PlanoFinanceiro> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<PlanoFinanceiro> findByTenantIdAndDeletedFalseAndAtivoTrue(UUID tenantId);
}
