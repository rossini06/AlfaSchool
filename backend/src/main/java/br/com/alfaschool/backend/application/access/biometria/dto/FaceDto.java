package br.com.alfaschool.backend.application.access.biometria.dto;

import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;

import java.time.Instant;
import java.util.UUID;

/**
 * Visao da face para a tela. NAO carrega a foto nem a chave do
 * armazenamento: a imagem tem rota propria, com autorizacao propria.
 */
public record FaceDto(
        UUID id,
        TitularTipo titularTipo,
        UUID titularId,
        long deviceUserId,
        String baseLegal,
        boolean consentimentoObtido,
        Instant consentimentoEm,
        String consentimentoVersao,
        boolean ativo,
        boolean exportavel,
        Instant atualizadoEm
) {
    public static FaceDto from(AccFace f) {
        return new FaceDto(
                f.getId(),
                f.getTitularTipo(),
                f.getTitularId(),
                f.getDeviceUserId() == null ? 0L : f.getDeviceUserId(),
                f.getBaseLegal(),
                f.isConsentimentoObtido(),
                f.getConsentimentoEm(),
                f.getConsentimentoVersao(),
                f.isAtivo(),
                f.exportavel(),
                f.getUpdatedAt());
    }
}
