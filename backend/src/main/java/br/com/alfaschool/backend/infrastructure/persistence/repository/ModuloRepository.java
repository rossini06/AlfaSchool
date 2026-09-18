package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.modulo.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuloRepository extends JpaRepository<Modulo, UUID> {
    Optional<Modulo> findByCodigo(String codigo);
    List<Modulo> findAllByOrderByOrdemAsc();
}
