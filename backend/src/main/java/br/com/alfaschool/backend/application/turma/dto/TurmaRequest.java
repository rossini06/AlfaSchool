package br.com.alfaschool.backend.application.turma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record TurmaRequest(
    @NotNull UUID cursoId,
    @NotBlank String nome,
    String codigo,
    @NotNull Integer anoLetivo,
    String turno,
    String professorResponsavel,
    Integer capacidadeMaxima,
    LocalDate dataInicio,
    LocalDate dataFim,
    String status,
    UUID unitId,
    Boolean ativa
) {}
