package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.permanencia.AccPresenca;
import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;
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

/**
 * TODA consulta agregada daqui exclui StatusPresenca.INCONSISTENTE na
 * propria JPQL, e nao por parametro: um dia com marcacao faltando tem
 * numero de fantasia (quem esqueceu de registrar a saida "ficou" ate a
 * meia-noite) e esse numero vira cobranca. A regra precisa ser impossivel
 * de esquecer no lado de quem chama.
 */
@Repository
public interface AccPresencaRepository extends JpaRepository<AccPresenca, UUID> {

    /** Totais de um aluno num periodo, ja sem os dias inconsistentes. */
    interface TotaisAluno {
        UUID getAlunoId();
        Long getDias();
        Long getMinutosPermanencia();
        Long getMinutosPrevistos();
        Long getMinutosExcedente();
        Long getMinutosAntecipacao();
    }

    Optional<AccPresenca> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<AccPresenca> findByTenantIdAndAlunoIdAndDataAndDeletedFalse(UUID tenantId, UUID alunoId, LocalDate data);

    List<AccPresenca> findByTenantIdAndAlunoIdAndDataBetweenAndDeletedFalseOrderByDataAsc(
            UUID tenantId, UUID alunoId, LocalDate inicio, LocalDate fim);

    Page<AccPresenca> findByTenantIdAndAlunoIdAndDataBetweenAndDeletedFalse(
            UUID tenantId, UUID alunoId, LocalDate inicio, LocalDate fim, Pageable pageable);

    List<AccPresenca> findByTenantIdAndDataAndStatusAndDeletedFalse(UUID tenantId, LocalDate data,
                                                                   StatusPresenca status);

    /** Dias que ficaram em aberto ate a data — insumo do job de fechamento diario. */
    List<AccPresenca> findByTenantIdAndStatusAndDataLessThanEqualAndDeletedFalse(
            UUID tenantId, StatusPresenca status, LocalDate limite);

    List<AccPresenca> findByStatusAndDataAndDeletedFalse(StatusPresenca status, LocalDate data);

    List<AccPresenca> findByStatusAndDataLessThanEqualAndDeletedFalse(StatusPresenca status, LocalDate limite);

    /** Quem esta na unidade agora. unitId/alunos nulos = sem filtro. */
    @Query("""
            select p from AccPresenca p
            where p.tenantId = :tenantId
              and p.data = :data
              and p.status = br.com.alfaschool.backend.domain.access.shared.StatusPresenca.ABERTA
              and p.deleted = false
              and (:unitId is null or p.unitId = :unitId)
              and (:semFiltroAluno = true or p.alunoId in :alunoIds)
            order by p.primeiraEntradaEm asc
            """)
    Page<AccPresenca> buscarAbertas(@Param("tenantId") UUID tenantId,
                                    @Param("data") LocalDate data,
                                    @Param("unitId") UUID unitId,
                                    @Param("semFiltroAluno") boolean semFiltroAluno,
                                    @Param("alunoIds") List<UUID> alunoIds,
                                    Pageable pageable);

    @Query("""
            select p.alunoId as alunoId,
                   count(p) as dias,
                   coalesce(sum(p.minutosPermanencia), 0) as minutosPermanencia,
                   coalesce(sum(p.minutosPrevistos), 0) as minutosPrevistos,
                   coalesce(sum(p.minutosExcedente), 0) as minutosExcedente,
                   coalesce(sum(p.minutosAntecipacao), 0) as minutosAntecipacao
            from AccPresenca p
            where p.tenantId = :tenantId
              and p.data between :inicio and :fim
              and p.deleted = false
              and p.status <> br.com.alfaschool.backend.domain.access.shared.StatusPresenca.INCONSISTENTE
              and (:unitId is null or p.unitId = :unitId)
              and (:semFiltroAluno = true or p.alunoId in :alunoIds)
            group by p.alunoId
            """)
    List<TotaisAluno> totalizarPorAluno(@Param("tenantId") UUID tenantId,
                                        @Param("inicio") LocalDate inicio,
                                        @Param("fim") LocalDate fim,
                                        @Param("unitId") UUID unitId,
                                        @Param("semFiltroAluno") boolean semFiltroAluno,
                                        @Param("alunoIds") List<UUID> alunoIds);

    /** So' quem tem excedente > 0. Ordenacao fica por conta do service. */
    @Query("""
            select p.alunoId as alunoId,
                   count(p) as dias,
                   coalesce(sum(p.minutosPermanencia), 0) as minutosPermanencia,
                   coalesce(sum(p.minutosPrevistos), 0) as minutosPrevistos,
                   coalesce(sum(p.minutosExcedente), 0) as minutosExcedente,
                   coalesce(sum(p.minutosAntecipacao), 0) as minutosAntecipacao
            from AccPresenca p
            where p.tenantId = :tenantId
              and p.data between :inicio and :fim
              and p.deleted = false
              and p.status <> br.com.alfaschool.backend.domain.access.shared.StatusPresenca.INCONSISTENTE
              and (:unitId is null or p.unitId = :unitId)
              and (:semFiltroAluno = true or p.alunoId in :alunoIds)
            group by p.alunoId
            having sum(p.minutosExcedente) > 0
            """)
    List<TotaisAluno> totalizarExcedentes(@Param("tenantId") UUID tenantId,
                                          @Param("inicio") LocalDate inicio,
                                          @Param("fim") LocalDate fim,
                                          @Param("unitId") UUID unitId,
                                          @Param("semFiltroAluno") boolean semFiltroAluno,
                                          @Param("alunoIds") List<UUID> alunoIds);

    /** Presencas do periodo de uma unidade — usadas pelo congelamento. */
    @Query("""
            select p from AccPresenca p
            where p.tenantId = :tenantId
              and p.data between :inicio and :fim
              and p.deleted = false
              and (:unitId is null or p.unitId = :unitId)
            """)
    List<AccPresenca> buscarDoPeriodo(@Param("tenantId") UUID tenantId,
                                      @Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim,
                                      @Param("unitId") UUID unitId);

    /** Alunos que ja tem presenca no periodo — base do recalculo forcado. */
    @Query("""
            select distinct p.alunoId from AccPresenca p
            where p.tenantId = :tenantId
              and p.data between :inicio and :fim
              and p.deleted = false
            """)
    List<UUID> findAlunoIdsComPresencaNoPeriodo(@Param("tenantId") UUID tenantId,
                                                @Param("inicio") LocalDate inicio,
                                                @Param("fim") LocalDate fim);
}
