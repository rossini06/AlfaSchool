package br.com.alfaschool.backend.application.access.autorizacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo das transicoes que EXIGEM justificativa (suspender, revogar).
 * Aprovar e reativar usam MotivoOpcionalRequest.
 */
public record MotivoRequest(
        @NotBlank(message = "Motivo e obrigatorio") @Size(max = 255) String motivo
) {
}
