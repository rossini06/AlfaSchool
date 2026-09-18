package br.com.alfaschool.backend.application.access.autorizacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * As tres permissoes chegam separadas e sem default implicito no request.
 * Quando nulas o service aplica o default do schema (podeRetirar=true,
 * as outras duas false) — jamais copia uma da outra.
 */
public record PessoaAutorizadaRequest(
        UUID responsavelId,
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 14) String cpf,
        @Size(max = 20) String rg,
        @Size(max = 20) String telefone,
        @Email @Size(max = 160) String email,
        @Size(max = 255) String fotoKey,
        Boolean podeRetirar,
        Boolean podeAcessarPortal,
        Boolean recebeNotificacao,
        Boolean ativo
) {
}
