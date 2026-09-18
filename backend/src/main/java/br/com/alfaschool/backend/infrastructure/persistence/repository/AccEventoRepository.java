package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.evento.AccEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccEventoRepository extends JpaRepository<AccEvento, UUID> {

    /**
     * Candidatos a replay: mesmo (dispositivo, device_log_id) dentro de uma
     * janela de tempo curta.
     *
     * NAO existe UNIQUE em (dispositivo_id, device_log_id) no banco, e isso
     * e' deliberado: o Control iD REINICIA o contador de log quando o
     * historico e' limpo no equipamento. Com UNIQUE, todo evento apos a
     * limpeza colidiria com um evento antigo e seria perdido para sempre —
     * um buraco negro silencioso no livro-razao da escola. A janela de tempo
     * distingue "e' o mesmo evento chegando duas vezes" de "o contador
     * voltou a zero e este e' um evento novo".
     */
    @Query("""
            select e from AccEvento e
            where e.dispositivoId = :dispositivoId
              and e.deviceLogId = :deviceLogId
              and e.dataHora between :inicio and :fim
            order by e.dataHora desc
            """)
    List<AccEvento> buscarReplay(@Param("dispositivoId") UUID dispositivoId,
                                 @Param("deviceLogId") Long deviceLogId,
                                 @Param("inicio") Instant inicio,
                                 @Param("fim") Instant fim);

    Page<AccEvento> findByTenantIdOrderByDataHoraDesc(UUID tenantId, Pageable pageable);

    List<AccEvento> findByTenantIdAndDataHoraBetweenOrderByDataHoraAsc(
            UUID tenantId, Instant inicio, Instant fim);

    java.util.Optional<AccEvento> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndDispositivoId(UUID tenantId, UUID dispositivoId);
}
