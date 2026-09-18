package br.com.alfaschool.backend.application.access.retirada.dto;

import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OcorrenciaRequest(
        UUID unitId,
        @NotNull(message = "Informe o tipo da ocorrencia") TipoOcorrencia tipo,
        GravidadeOcorrencia gravidade,
        UUID alunoId,
        UUID pessoaAutorizadaId,
        UUID dispositivoId,
        UUID retiradaId,
        @NotBlank(message = "Descreva a ocorrencia") String descricao
) {
}
