package br.com.alfaschool.backend.application.dispositivo.dto;

import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ModoSync;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;

import java.time.Instant;
import java.util.UUID;

/**
 * Note o que NAO sai daqui: login, senha cifrada e token de webhook. Sao
 * credenciais de equipamento — quem precisa troca-las usa o endpoint
 * proprio, e quem so' lista nao precisa ve-las.
 */
public record DispositivoResponse(
    UUID id, UUID tenantId, UUID unitId, String nome, String tipo,
    String fabricante, String modelo, String ip, Integer porta, String serial,
    UUID portariaId, FuncaoDispositivo funcao, SentidoAcesso sentido, ModoSync modoSync,
    boolean ativo, boolean online, Instant ultimoPing, Instant ultimoHeartbeat,
    Instant createdAt, Instant updatedAt
) {
    public static DispositivoResponse from(Dispositivo d) {
        return new DispositivoResponse(
            d.getId(), d.getTenantId(), d.getUnitId(), d.getNome(), d.getTipo(),
            d.getFabricante(), d.getModelo(), d.getIp(), d.getPorta(), d.getSerial(),
            d.getPortariaId(), d.getFuncao(), d.getSentido(), d.getModoSync(),
            d.isAtivo(), d.isOnline(), d.getUltimoPing(), d.getUltimoHeartbeat(),
            d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
