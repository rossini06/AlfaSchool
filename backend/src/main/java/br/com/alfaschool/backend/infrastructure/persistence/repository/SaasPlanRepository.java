package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.saas.SaasPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaasPlanRepository extends JpaRepository<SaasPlan, UUID> {
    List<SaasPlan> findByAtivoTrueAndDeletedFalse();
    List<SaasPlan> findByDeletedFalse();
}
