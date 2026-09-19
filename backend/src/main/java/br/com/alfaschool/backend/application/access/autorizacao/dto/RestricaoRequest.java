package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.domain.access.autorizacao.TipoRestricao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A pessoa restrita pode nao estar cadastrada: informe pessoaAutorizadaId
 * OU pessoaNome+pessoaCpf. O CPF e' o que continua bloqueando se ela vier a
 * se cadastrar depois, por isso vale sempre registra-lo quando conhecido.
 */
public record RestricaoRequest(
        @NotNull UUID alunoId,
        UUID pessoaAutorizadaId,
        @Size(max = 120) String pessoaNome,
        @Size(max = 14) String pessoaCpf,
        TipoRestricao tipo,
        @Size(max = 60) String numeroProcesso,
        @Size(max = 120) String orgaoEmissor,
        @NotBlank String descricao,
        @Size(max = 255) String documentoKey,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        Boolean ativo
) {
}
