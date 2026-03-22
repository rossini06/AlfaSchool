package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponsavelRepository extends JpaRepository<Responsavel, UUID> {

    List<Responsavel> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    Page<Responsavel> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    @Query("SELECT r FROM Responsavel r WHERE r.tenantId = :tenantId AND r.deleted = false AND (LOWER(r.nome) LIKE LOWER(CONCAT('%', :search, '%')) OR r.cpf LIKE CONCAT('%', :search, '%') OR LOWER(r.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Responsavel> searchByNome(UUID tenantId, String search, Pageable pageable);

    boolean existsByTenantIdAndCpfAndDeletedFalse(UUID tenantId, String cpf);
    boolean existsByTenantIdAndCpfAndDeletedFalseAndIdNot(UUID tenantId, String cpf, UUID id);
}
