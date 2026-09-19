package br.com.alfaschool.backend.application.curso.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CursoRequest(
    @NotBlank(message = "Informe o nome do curso") @Size(max = 160, message = "Nome deve ter no máximo 160 caracteres") String nome,
    @Size(max = 20, message = "Código deve ter no máximo 20 caracteres") String codigo,
    String descricao,
    @Min(value = 1, message = "Carga horária deve ser maior que zero") Integer cargaHoraria,
    @Size(max = 20, message = "Modalidade deve ter no máximo 20 caracteres") String modalidade,
    @Size(max = 30, message = "Nível deve ter no máximo 30 caracteres") String nivel,
    @Size(max = 30, message = "Tipo deve ter no máximo 30 caracteres") String tipo,
    Integer duracaoMeses,
    Integer idadeMinima,
    Integer idadeMaxima,
    BigDecimal precoBase,
    @DecimalMin(value = "0.0", message = "Nota mínima não pode ser negativa")
    @DecimalMax(value = "10.0", message = "Nota mínima não pode passar de 10") BigDecimal notaMinimaAprovacao,
    @DecimalMin(value = "0.0", message = "Frequência mínima não pode ser negativa")
    @DecimalMax(value = "100.0", message = "Frequência mínima não pode passar de 100%") BigDecimal frequenciaMinimaAprovacao,
    UUID unitId,
    Boolean ativo
) {}
