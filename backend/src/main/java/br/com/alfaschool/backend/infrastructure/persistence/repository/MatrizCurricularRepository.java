package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.matriz.MatrizCurricular;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatrizCurricularRepository extends JpaRepository<MatrizCurricular, UUID> {

    List<MatrizCurricular> findByTenantIdAndCursoIdAndDeletedFalse(UUID tenantId, UUID cursoId);

    boolean existsByTenantIdAndCursoIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
            UUID tenantId, UUID cursoId, UUID disciplinaId, String periodo);
}
