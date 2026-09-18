package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.domain.access.shared.TipoPainel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record PainelRequest(
        @NotNull(message = "Informe a unidade") UUID unitId,
        @NotBlank(message = "Informe o nome do painel") String nome,
        /** Entra na URL permanente da TV: so' minusculas, numeros e hifen. */
        @NotBlank(message = "Informe o slug do painel")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,59}$",
                 message = "Slug deve ter apenas minusculas, numeros e hifen")
        String slug,
        @NotNull(message = "Informe o tipo do painel") TipoPainel tipo,
        Boolean exibeFoto,
        /** Segundos que o cartao fica na tela apos a entrega. */
        @Min(value = 0, message = "Retencao nao pode ser negativa")
        @Max(value = 600, message = "Retencao maxima e de 600 segundos")
        Integer retencaoSeg,
        Boolean ativo
) {
}
