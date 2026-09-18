package br.com.alfaschool.backend.application.access.autorizacao.dto;

import jakarta.validation.constraints.Size;

/** Corpo das transicoes em que a justificativa e' bem-vinda mas nao exigida. */
public record MotivoOpcionalRequest(
        @Size(max = 255) String motivo
) {
}
