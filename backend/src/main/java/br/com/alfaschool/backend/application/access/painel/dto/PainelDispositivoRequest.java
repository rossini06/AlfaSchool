package br.com.alfaschool.backend.application.access.painel.dto;

import jakarta.validation.constraints.NotBlank;

/** Nome identifica a TV para a coordenacao ("TV corredor 2o andar"). */
public record PainelDispositivoRequest(
        @NotBlank(message = "Informe o nome do dispositivo") String nome
) {
}
