package br.com.alfaschool.backend.application.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record TenantStatusRequest(
    @NotBlank String status
) {}
