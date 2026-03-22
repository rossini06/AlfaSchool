package br.com.alfaschool.backend.application.curso.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CursoRequest(
    @NotBlank @Size(max = 255) String nome,
    String codigo,
    String descricao,
    @Min(1) Integer cargaHoraria,
    String modalidade,
    String nivel,
    String tipo,
    Integer duracaoMeses,
    Integer idadeMinima,
    Integer idadeMaxima,
    BigDecimal precoBase,
    @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal notaMinimaAprovacao,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal frequenciaMinimaAprovacao,
    UUID unitId,
    Boolean ativo
) {}
