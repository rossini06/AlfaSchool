package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.retirada.AccRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccRetiradaRepository extends JpaRepository<AccRetirada, UUID> {

    Optional<AccRetirada> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    /**
     * Proxima posicao na fila do dia. A fila e' por ordem de chegada dentro
     * da unidade: duas unidades da mesma rede numeram em paralelo, senao o
     * pai da unidade B ouviria "voce e' o 47o" num dia de 12 retiradas.
     *
     * unitId entra como parametro repetido para tratar unidade nula (escola
     * de uma unidade so') sem quebrar a comparacao.
     */
    @Query("""
           select coalesce(max(r.ordemChegada), 0)
           from AccRetirada r
           where r.tenantId = :tenantId
             and (:unitId is null or r.unitId = :unitId)
             and r.solicitadoEm >= :inicioDoDia
             and r.solicitadoEm < :fimDoDia
           """)
    int maiorOrdemChegadaDoDia(@Param("tenantId") UUID tenantId,
                               @Param("unitId") UUID unitId,
                               @Param("inicioDoDia") Instant inicioDoDia,
                               @Param("fimDoDia") Instant fimDoDia);

    /**
     * Evita abrir duas retiradas para o mesmo aluno quando o responsavel
     * encosta o rosto duas vezes no leitor.
     */
    @Query("""
           select r from AccRetirada r
           where r.tenantId = :tenantId
             and r.alunoId = :alunoId
             and r.deleted = false
             and r.status in :statusAbertos
           """)
    List<AccRetirada> abertasDoAluno(@Param("tenantId") UUID tenantId,
                                     @Param("alunoId") UUID alunoId,
                                     @Param("statusAbertos") Collection<StatusRetirada> statusAbertos);

    List<AccRetirada> findByTenantIdAndAlunoIdAndDeletedFalseOrderBySolicitadoEmDesc(UUID tenantId, UUID alunoId);

    /** Contador agregado do painel de coordenacao: nao conta lista em Java. */
    @Query("""
           select count(r) from AccRetirada r
           where r.tenantId = :tenantId
             and (:unitId is null or r.unitId = :unitId)
             and r.deleted = false
             and r.status in :status
             and r.solicitadoEm >= :inicioDoDia
             and r.solicitadoEm < :fimDoDia
           """)
    long contarPorStatusNoDia(@Param("tenantId") UUID tenantId,
                              @Param("unitId") UUID unitId,
                              @Param("status") Collection<StatusRetirada> status,
                              @Param("inicioDoDia") Instant inicioDoDia,
                              @Param("fimDoDia") Instant fimDoDia);

    /** Saidas efetivas do dia — saidaEm, nunca entregueEm. */
    @Query("""
           select count(r) from AccRetirada r
           where r.tenantId = :tenantId
             and (:unitId is null or r.unitId = :unitId)
             and r.deleted = false
             and r.saidaEm >= :inicioDoDia
             and r.saidaEm < :fimDoDia
           """)
    long contarSaidasNoDia(@Param("tenantId") UUID tenantId,
                           @Param("unitId") UUID unitId,
                           @Param("inicioDoDia") Instant inicioDoDia,
                           @Param("fimDoDia") Instant fimDoDia);

    /**
     * Quem esta' esperando ha mais de N minutos. O corte vem como Instant ja
     * calculado para que o banco compare data com data.
     */
    @Query("""
           select count(r) from AccRetirada r
           where r.tenantId = :tenantId
             and (:unitId is null or r.unitId = :unitId)
             and r.deleted = false
             and r.status in :status
             and r.solicitadoEm < :corte
           """)
    long contarEsperandoAlemDe(@Param("tenantId") UUID tenantId,
                               @Param("unitId") UUID unitId,
                               @Param("status") Collection<StatusRetirada> status,
                               @Param("corte") Instant corte);
}
