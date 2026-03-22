package br.com.alfaschool.backend.application.matricula.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MatriculaRequest(
    @NotNull UUID alunoId,
    @NotNull UUID turmaId,
    LocalDate dataMatricula,
    LocalDate dataConclusao,
    String obs,
    String tipo,
    String statusAcademico,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal desconto,
    UUID unitId
) {}
