package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.disciplina.Disciplina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisciplinaRepository extends JpaRepository<Disciplina, UUID> {

    Page<Disciplina> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    @Query("SELECT d FROM Disciplina d WHERE d.tenantId = :tenantId AND d.deleted = false AND LOWER(d.nome) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Disciplina> search(UUID tenantId, String q, Pageable pageable);

    List<Disciplina> findByTenantIdAndDeletedFalseAndAtivaTrue(UUID tenantId);
}
