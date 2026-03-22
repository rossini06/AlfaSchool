package br.com.alfaschool.backend.application.dispositivo.dto;

import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import java.time.Instant;
import java.util.UUID;

public record DispositivoResponse(
    UUID id, UUID tenantId, UUID unitId, String nome, String tipo,
    String fabricante, String modelo, String ip, Integer porta, String serial,
    boolean ativo, boolean online, Instant ultimoPing,
    Instant createdAt, Instant updatedAt
) {
    public static DispositivoResponse from(Dispositivo d) {
        return new DispositivoResponse(
            d.getId(), d.getTenantId(), d.getUnitId(), d.getNome(), d.getTipo(),
            d.getFabricante(), d.getModelo(), d.getIp(), d.getPorta(), d.getSerial(),
            d.isAtivo(), d.isOnline(), d.getUltimoPing(),
            d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
