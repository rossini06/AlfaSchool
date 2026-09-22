package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.application.access.biometria.FotoUrlAssinada;
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
        String fotoUrl,
        String observacoes,
        boolean podeRetirar,
        boolean podeAcessarPortal,
        boolean recebeNotificacao,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    /** Sem assinador: fotoUrl fica nula (a foto so' carrega com a URL assinada). */
    public static PessoaAutorizadaResponse from(PessoaAutorizada p) {
        return from(p, null);
    }

    /**
     * fotoKey e' uma chave de storage, nao um endereco servivel: a imagem so'
     * sai pelo endpoint assinado. Por isso a lista/edicao precisam da fotoUrl
     * emitida aqui — sem ela, a tela renderiza uma img quebrada.
     */
    public static PessoaAutorizadaResponse from(PessoaAutorizada p, FotoUrlAssinada assinador) {
        return new PessoaAutorizadaResponse(
                p.getId(), p.getTenantId(), p.getResponsavelId(), p.getNome(),
                p.getParentesco(), p.getCpf(), p.getRg(), p.getTelefone(), p.getEmail(),
                p.getFotoKey(),
                assinador != null ? assinador.emitir(p.getFotoKey()) : null,
                p.getObservacoes(),
                p.isPodeRetirar(), p.isPodeAcessarPortal(), p.isRecebeNotificacao(),
                p.isAtivo(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
