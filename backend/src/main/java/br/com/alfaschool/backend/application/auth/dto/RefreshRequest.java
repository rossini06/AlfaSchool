package br.com.alfaschool.backend.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken é obrigatório")
        String refreshToken
) {
}
