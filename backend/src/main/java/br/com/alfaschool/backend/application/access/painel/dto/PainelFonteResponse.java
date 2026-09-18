package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.domain.access.painel.AccPainelFonte;
import br.com.alfaschool.backend.domain.access.shared.EscopoPainel;

import java.util.UUID;

public record PainelFonteResponse(
        UUID id,
        UUID painelId,
        EscopoPainel escopo,
        UUID referenciaId
) {
    public static PainelFonteResponse from(AccPainelFonte f) {
        return new PainelFonteResponse(f.getId(), f.getPainelId(), f.getEscopo(), f.getReferenciaId());
    }
}
