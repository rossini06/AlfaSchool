package br.com.alfaschool.backend.application.access.retirada.dto;

import jakarta.validation.constraints.NotBlank;

/** Cancelar e negar exigem motivo: sem ele a fila vira caixa-preta. */
public record MotivoRequest(
        @NotBlank(message = "Informe o motivo") String motivo
) {
}
