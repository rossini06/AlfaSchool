package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import br.com.alfaschool.backend.domain.access.autorizacao.TipoRestricao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * documentoKey NAO aparece aqui. O mandado/decisao fica em storage restrito
 * e so sai por GET /{id}/documento, com @PreAuthorize mais estrito. Quem
 * atende a portaria precisa saber que existe restricao, nao ler o processo.
 * O campo temDocumento existe para a tela saber se ha o que pedir.
 */
public record RestricaoResponse(
        UUID id,
        UUID tenantId,
        UUID alunoId,
        UUID pessoaAutorizadaId,
        String pessoaNome,
        String pessoaCpf,
        TipoRestricao tipo,
        String descricao,
        boolean temDocumento,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        boolean ativo,
        UUID registradoPorUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static RestricaoResponse from(Restricao r) {
        return new RestricaoResponse(
                r.getId(), r.getTenantId(), r.getAlunoId(), r.getPessoaAutorizadaId(),
                r.getPessoaNome(), r.getPessoaCpf(), r.getTipo(), r.getDescricao(),
                r.getDocumentoKey() != null && !r.getDocumentoKey().isBlank(),
                r.getVigenciaInicio(), r.getVigenciaFim(), r.isAtivo(),
                r.getRegistradoPorUserId(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
