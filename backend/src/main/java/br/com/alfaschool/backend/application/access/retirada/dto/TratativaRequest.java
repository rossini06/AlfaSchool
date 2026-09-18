package br.com.alfaschool.backend.application.access.retirada.dto;

import jakarta.validation.constraints.NotBlank;

public record TratativaRequest(
        @NotBlank(message = "Descreva a tratativa") String tratativa
) {
}
