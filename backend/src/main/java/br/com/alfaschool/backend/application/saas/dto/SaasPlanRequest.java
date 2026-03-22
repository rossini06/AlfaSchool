package br.com.alfaschool.backend.application.saas.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record SaasPlanRequest(
    @NotBlank String nome,
    @NotBlank String slug,
    String descricao,
    BigDecimal precoMensal,
    BigDecimal precoAnual,
    Integer maxEscolas,
    Integer maxUsuarios,
    Integer maxDispositivos,
    String recursos,
    Boolean ativo
) {}
