package br.com.alfaschool.backend.application.disciplina.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record DisciplinaRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 255)
        String nome,
        String codigo,
        Integer cargaHoraria,
        String descricao,
        UUID cursoId,
        String tipo,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal notaMaxima,
        @DecimalMin("0.0") BigDecimal peso,
        Boolean permiteRecuperacao,
        String tipoAvaliacao,
        Boolean ativa
) {}
