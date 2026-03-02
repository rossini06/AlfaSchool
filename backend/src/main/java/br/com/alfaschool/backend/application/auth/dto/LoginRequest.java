package br.com.alfaschool.backend.application.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LoginRequest(
        UUID tenantId,

        @Email(message = "Informe um e-mail válido")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
