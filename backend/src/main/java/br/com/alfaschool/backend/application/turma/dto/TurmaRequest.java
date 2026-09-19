package br.com.alfaschool.backend.application.turma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Size;

public record TurmaRequest(
    @NotNull(message = "Informe o curso da turma") UUID cursoId,
    @NotBlank(message = "Informe o nome da turma") @Size(max = 80, message = "Nome deve ter no máximo 80 caracteres") String nome,
    @Size(max = 20, message = "Código deve ter no máximo 20 caracteres") String codigo,
    @NotNull(message = "Informe o ano letivo") Integer anoLetivo,
    @Size(max = 20, message = "Turno deve ter no máximo 20 caracteres") String turno,
    @Size(max = 120, message = "Professor responsável deve ter no máximo 120 caracteres") String professorResponsavel,
    Integer capacidadeMaxima,
    LocalDate dataInicio,
    LocalDate dataFim,
    @Size(max = 20, message = "Status deve ter no máximo 20 caracteres") String status,
    UUID unitId,
    Boolean ativa
) {}
