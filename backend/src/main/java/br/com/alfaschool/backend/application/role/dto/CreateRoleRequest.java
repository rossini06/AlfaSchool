package br.com.alfaschool.backend.application.role.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank(message = "Nome da role é obrigatório")
        String name,

        String description
) {
}
