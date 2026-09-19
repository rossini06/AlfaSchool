package br.com.alfaschool.backend.application.matricula.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Size;

public record MatriculaRequest(
    @NotNull(message = "Informe o aluno") UUID alunoId,
    @NotNull(message = "Informe a turma") UUID turmaId,
    LocalDate dataMatricula,
    LocalDate dataConclusao,
    String obs,
    @Size(max = 30, message = "Tipo deve ter no máximo 30 caracteres") String tipo,
    @Size(max = 30, message = "Status acadêmico deve ter no máximo 30 caracteres") String statusAcademico,
    @DecimalMin(value = "0.0", message = "Desconto não pode ser negativo")
    @DecimalMax(value = "100.0", message = "Desconto não pode passar de 100%") BigDecimal desconto,
    UUID unitId
) {}
