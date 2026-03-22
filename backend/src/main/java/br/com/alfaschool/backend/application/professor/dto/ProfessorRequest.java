package br.com.alfaschool.backend.application.professor.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;

public record ProfessorRequest(
        UUID unitId,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String cpf,
        String email,
        String telefone,
        String especialidade,
        LocalDate dataNascimento,
        LocalDate dataContratacao,
        String status
) {}
