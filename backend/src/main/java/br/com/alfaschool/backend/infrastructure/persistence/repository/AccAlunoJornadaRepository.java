package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.jornada.AccAlunoJornada;
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
public interface AccAlunoJornadaRepository extends JpaRepository<AccAlunoJornada, UUID> {

    Optional<AccAlunoJornada> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Page<AccAlunoJornada> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId, Pageable pageable);

    List<AccAlunoJornada> findByTenantIdAndAlunoIdAndDeletedFalseOrderByVigenciaInicioDesc(UUID tenantId, UUID alunoId);

    /**
     * Vinculos vigentes na data. Ordenado do mais recente para o mais
     * antigo: se por algum motivo houver mais de um (dado legado anterior
     * a' validacao de sobreposicao), vence o de inicio mais recente.
     */
    @Query("""
            select v from AccAlunoJornada v
            where v.tenantId = :tenantId
              and v.alunoId = :alunoId
              and v.deleted = false
              and v.vigenciaInicio <= :data
              and (v.vigenciaFim is null or v.vigenciaFim >= :data)
            order by v.vigenciaInicio desc
            """)
    List<AccAlunoJornada> findVigentesEm(@Param("tenantId") UUID tenantId,
                                         @Param("alunoId") UUID alunoId,
                                         @Param("data") LocalDate data);

    /**
     * Sobreposicao de intervalos: [a.inicio, a.fim] e [b.inicio, b.fim] se
     * cruzam quando a.inicio <= b.fim e b.inicio <= a.fim, tratando fim
     * nulo como infinito. Usado para barrar dois vinculos vigentes ao
     * mesmo tempo (409).
     */
    @Query("""
            select v from AccAlunoJornada v
            where v.tenantId = :tenantId
              and v.alunoId = :alunoId
              and v.deleted = false
              and (:ignorarId is null or v.id <> :ignorarId)
              and (:fim is null or v.vigenciaInicio <= :fim)
              and (v.vigenciaFim is null or v.vigenciaFim >= :inicio)
            """)
    List<AccAlunoJornada> findSobrepostos(@Param("tenantId") UUID tenantId,
                                          @Param("alunoId") UUID alunoId,
                                          @Param("inicio") LocalDate inicio,
                                          @Param("fim") LocalDate fim,
                                          @Param("ignorarId") UUID ignorarId);

    /** Quantos alunos ainda dependem desta jornada na data. */
    @Query("""
            select count(v) from AccAlunoJornada v
            where v.tenantId = :tenantId
              and v.jornadaId = :jornadaId
              and v.deleted = false
              and v.vigenciaInicio <= :data
              and (v.vigenciaFim is null or v.vigenciaFim >= :data)
            """)
    long contarVigentesDaJornada(@Param("tenantId") UUID tenantId,
                                 @Param("jornadaId") UUID jornadaId,
                                 @Param("data") LocalDate data);

    /** Alunos que tem qualquer vinculo cruzando o periodo — base do reprocessamento. */
    @Query("""
            select distinct v.alunoId from AccAlunoJornada v
            where v.tenantId = :tenantId
              and v.deleted = false
              and v.vigenciaInicio <= :fim
              and (v.vigenciaFim is null or v.vigenciaFim >= :inicio)
            """)
    List<UUID> findAlunoIdsComJornadaNoPeriodo(@Param("tenantId") UUID tenantId,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);
}
