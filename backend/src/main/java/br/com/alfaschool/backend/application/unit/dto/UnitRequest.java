package br.com.alfaschool.backend.application.unit.dto;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

public record UnitRequest(
    @NotBlank(message = "Informe o nome da unidade") @Size(max = 160, message = "Nome deve ter no máximo 160 caracteres") String name,
    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres") String address,
    @Size(max = 120, message = "Cidade deve ter no máximo 120 caracteres") String city,
    @Size(max = 2, message = "Estado deve ser a sigla com 2 letras") String state,
    @Size(max = 9, message = "CEP deve ter no máximo 9 caracteres") String cep,
    @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres") String email,
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres") String telefone,
    Boolean active
) {}
