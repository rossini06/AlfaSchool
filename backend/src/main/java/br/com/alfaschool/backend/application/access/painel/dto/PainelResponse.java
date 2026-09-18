package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.shared.TipoPainel;

import java.util.List;
import java.util.UUID;

public record PainelResponse(
        UUID id,
        UUID unitId,
        String nome,
        String slug,
        TipoPainel tipo,
        boolean exibeFoto,
        int retencaoSeg,
        boolean ativo,
        List<PainelFonteResponse> fontes
) {
    public static PainelResponse from(AccPainel p) {
        return from(p, List.of());
    }

    public static PainelResponse from(AccPainel p, List<PainelFonteResponse> fontes) {
        return new PainelResponse(
                p.getId(),
                p.getUnitId(),
                p.getNome(),
                p.getSlug(),
                p.getTipo(),
                p.isExibeFoto(),
                p.getRetencaoSeg(),
                p.isAtivo(),
                fontes);
    }
}
