package br.com.alfaschool.backend.application.access.jornada.dto;

import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JornadaRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 255) String descricao,
        @Min(0) Integer toleranciaEntradaMin,
        @Min(0) Integer toleranciaSaidaMin,
        RegraExcedente regraExcedente,
        Boolean ativo,
        @Valid List<JornadaDiaRequest> dias
) {
}
