package br.com.alfaschool.backend.application.unit.dto;

import jakarta.validation.constraints.NotBlank;

public record UnitRequest(
    @NotBlank String name,
    String address,
    String city,
    String state,
    Boolean active
) {}
