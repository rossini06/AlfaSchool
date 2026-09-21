package br.com.alfaschool.backend.application.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantUpdateRequest(
        @NotBlank(message = "O nome da rede é obrigatório") @Size(max = 160) String name,
        @NotBlank(message = "O CNPJ é obrigatório") @Size(max = 40) String document
) {
}
