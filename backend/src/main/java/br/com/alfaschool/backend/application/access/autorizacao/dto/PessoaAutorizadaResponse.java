package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;

import java.time.Instant;
import java.util.UUID;

public record PessoaAutorizadaResponse(
        UUID id,
        UUID tenantId,
        UUID responsavelId,
        String nome,
        String parentesco,
        String cpf,
        String rg,
        String telefone,
        String email,
        String fotoKey,
        String observacoes,
        boolean podeRetirar,
        boolean podeAcessarPortal,
        boolean recebeNotificacao,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    public static PessoaAutorizadaResponse from(PessoaAutorizada p) {
        return new PessoaAutorizadaResponse(
                p.getId(), p.getTenantId(), p.getResponsavelId(), p.getNome(),
                p.getParentesco(), p.getCpf(), p.getRg(), p.getTelefone(), p.getEmail(),
                p.getFotoKey(), p.getObservacoes(),
                p.isPodeRetirar(), p.isPodeAcessarPortal(), p.isRecebeNotificacao(),
                p.isAtivo(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
