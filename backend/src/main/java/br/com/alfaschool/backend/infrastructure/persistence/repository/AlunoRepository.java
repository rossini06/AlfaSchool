package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.aluno.Aluno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, UUID> {
    Page<Aluno> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Aluno a WHERE a.tenantId = :tenantId AND a.deleted = false AND (LOWER(a.nome) LIKE LOWER(CONCAT('%',:q,'%')) OR a.cpf LIKE CONCAT('%',:q,'%'))")
    Page<Aluno> search(UUID tenantId, String q, Pageable pageable);

    Optional<Aluno> findByTenantIdAndCpfAndDeletedFalse(UUID tenantId, String cpf);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
