package br.com.alfaschool.backend.application.responsavel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ResponsavelRequest(
        @NotNull(message = "Aluno é obrigatório") UUID alunoId,
        @NotBlank(message = "Nome é obrigatório") String nome,
        String cpf,
        String telefone,
        String email,
        String tipo,
        Boolean principal
) {}
