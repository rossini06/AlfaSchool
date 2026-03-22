package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.vinculo.ProfessorTurmaDisciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfessorTurmaDisciplinaRepository extends JpaRepository<ProfessorTurmaDisciplina, UUID> {

    List<ProfessorTurmaDisciplina> findByTenantIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID turmaId);

    List<ProfessorTurmaDisciplina> findByTenantIdAndProfessorIdAndDeletedFalse(UUID tenantId, UUID professorId);

    boolean existsByTenantIdAndProfessorIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
            UUID tenantId, UUID professorId, UUID turmaId, UUID disciplinaId);
}
