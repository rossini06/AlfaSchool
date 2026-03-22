package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.responsavel.AlunoResponsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlunoResponsavelRepository extends JpaRepository<AlunoResponsavel, UUID> {

    List<AlunoResponsavel> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    void deleteByAlunoIdAndResponsavelId(UUID alunoId, UUID responsavelId);
}
