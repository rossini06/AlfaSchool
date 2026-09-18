package br.com.alfaschool.backend.application.access.biometria.dto;

import br.com.alfaschool.backend.domain.access.biometria.AccFaceSync;
import br.com.alfaschool.backend.domain.access.shared.StatusFaceSync;

import java.time.Instant;
import java.util.UUID;

/**
 * Veredito da face num equipamento especifico.
 *
 * codigoErro/detalhe existem para a tela dizer "olhos fechados" em vez de
 * "erro ao sincronizar": a secretaria precisa saber o que refazer.
 */
public record FaceSyncDto(
        UUID id,
        UUID faceId,
        UUID dispositivoId,
        StatusFaceSync status,
        String codigoErro,
        String detalhe,
        int tentativas,
        Instant sincronizadoEm
) {
    public static FaceSyncDto from(AccFaceSync s) {
        return new FaceSyncDto(
                s.getId(), s.getFaceId(), s.getDispositivoId(), s.getStatus(),
                s.getCodigoErro(), s.getDetalhe(), s.getTentativas(), s.getSincronizadoEm());
    }
}
