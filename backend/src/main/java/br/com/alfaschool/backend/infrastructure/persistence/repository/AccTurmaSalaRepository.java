package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.estrutura.AccTurmaSala;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccTurmaSalaRepository extends JpaRepository<AccTurmaSala, UUID> {

    /** Os dois filtros que a tela oferecia e que o controller ignorava. */
    @org.springframework.data.jpa.repository.Query(
        "select v from AccTurmaSala v where v.tenantId = :tenantId and v.deleted = false "
      + "and (:turmaId is null or v.turmaId = :turmaId) "
      + "and (:salaId is null or v.salaId = :salaId)")
    org.springframework.data.domain.Page<AccTurmaSala> buscar(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("turmaId") UUID turmaId,
            @org.springframework.data.repository.query.Param("salaId") UUID salaId,
            org.springframework.data.domain.Pageable pageable);


    Page<AccTurmaSala> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Optional<AccTurmaSala> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    List<AccTurmaSala> findByTenantIdAndTurmaIdAndDeletedFalse(UUID tenantId, UUID turmaId);

    List<AccTurmaSala> findByTenantIdAndSalaIdAndDeletedFalse(UUID tenantId, UUID salaId);

    long countByTenantIdAndSalaIdAndDeletedFalse(UUID tenantId, UUID salaId);

    /**
     * Candidatos de uma turma para uma data. Filtra so vigencia no banco; dias
     * da semana (CSV) e faixa de horario ficam em memoria, porque comparar CSV
     * em SQL nao usa indice e o conjunto de vinculos por turma e' pequeno.
     */
    @Query("SELECT v FROM AccTurmaSala v WHERE v.tenantId = :tenantId AND v.deleted = false "
            + "AND v.turmaId = :turmaId AND v.vigenciaInicio <= :data "
            + "AND (v.vigenciaFim IS NULL OR v.vigenciaFim >= :data)")
    List<AccTurmaSala> candidatosPorTurma(@Param("tenantId") UUID tenantId,
                                          @Param("turmaId") UUID turmaId,
                                          @Param("data") LocalDate data);

    @Query("SELECT v FROM AccTurmaSala v WHERE v.tenantId = :tenantId AND v.deleted = false "
            + "AND v.salaId = :salaId AND v.vigenciaInicio <= :data "
            + "AND (v.vigenciaFim IS NULL OR v.vigenciaFim >= :data)")
    List<AccTurmaSala> candidatosPorSala(@Param("tenantId") UUID tenantId,
                                         @Param("salaId") UUID salaId,
                                         @Param("data") LocalDate data);
}
