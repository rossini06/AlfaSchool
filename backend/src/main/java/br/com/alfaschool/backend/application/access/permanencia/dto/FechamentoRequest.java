package br.com.alfaschool.backend.application.access.permanencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * @param unitId nulo fecha o tenant inteiro. A unicidade no banco e'
 *               (tenant, unit, competencia), entao fechar por unidade e
 *               fechar o tenant sao dois registros diferentes.
 */
public record FechamentoRequest(
        UUID unitId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "Competência deve ser YYYY-MM") String competencia
) {
}
