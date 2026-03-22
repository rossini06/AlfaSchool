package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.professor.Professor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

    Page<Professor> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Professor p WHERE p.tenantId = :tenantId AND p.deleted = false AND (LOWER(p.nome) LIKE LOWER(CONCAT('%',:q,'%')) OR p.cpf LIKE CONCAT('%',:q,'%') OR LOWER(p.email) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Professor> search(UUID tenantId, String q, Pageable pageable);

    Optional<Professor> findByTenantIdAndCpfAndDeletedFalse(UUID tenantId, String cpf);

    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
