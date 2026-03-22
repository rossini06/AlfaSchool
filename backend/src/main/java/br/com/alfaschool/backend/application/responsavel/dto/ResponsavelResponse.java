package br.com.alfaschool.backend.application.responsavel.dto;

import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import java.time.Instant;
import java.util.UUID;

public record ResponsavelResponse(
        UUID id, UUID tenantId, UUID alunoId,
        String nome, String cpf, String telefone, String email,
        String tipo, boolean principal,
        Instant createdAt, Instant updatedAt
) {
    public static ResponsavelResponse from(Responsavel r) {
        return new ResponsavelResponse(
                r.getId(), r.getTenantId(), r.getAlunoId(),
                r.getNome(), r.getCpf(), r.getTelefone(), r.getEmail(),
                r.getTipo(), r.isPrincipal(),
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
