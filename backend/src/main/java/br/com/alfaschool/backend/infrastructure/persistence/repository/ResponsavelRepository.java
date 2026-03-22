package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponsavelRepository extends JpaRepository<Responsavel, UUID> {

    List<Responsavel> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);
}
