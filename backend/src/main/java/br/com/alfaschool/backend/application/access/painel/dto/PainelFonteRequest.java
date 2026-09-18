package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.domain.access.shared.EscopoPainel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * O recorte do painel. referenciaId so' pode ser nulo em escopo UNIDADE,
 * que significa "a unidade deste painel".
 */
public record PainelFonteRequest(
        @NotNull(message = "Informe o escopo da fonte") EscopoPainel escopo,
        UUID referenciaId
) {
}
