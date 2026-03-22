package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.financeiro.Contrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, UUID> {

    Page<Contrato> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<Contrato> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    List<Contrato> findByTenantIdAndAlunoIdAndStatusAndDeletedFalse(UUID tenantId, UUID alunoId, String status);
}
