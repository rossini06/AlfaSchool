package br.com.alfaschool.backend.application.matricula.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record MatriculaRequest(
    @NotNull UUID alunoId,
    @NotNull UUID turmaId,
    LocalDate dataMatricula,
    LocalDate dataConclusao,
    String obs,
    UUID unitId
) {}
