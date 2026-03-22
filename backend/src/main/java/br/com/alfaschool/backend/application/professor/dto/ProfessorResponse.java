package br.com.alfaschool.backend.application.professor.dto;

import br.com.alfaschool.backend.domain.professor.Professor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfessorResponse(
        UUID id,
        UUID tenantId,
        UUID unitId,
        String nome,
        String cpf,
        String email,
        String telefone,
        String especialidade,
        LocalDate dataNascimento,
        LocalDate dataContratacao,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProfessorResponse from(Professor p) {
        return new ProfessorResponse(
                p.getId(), p.getTenantId(), p.getUnitId(),
                p.getNome(), p.getCpf(), p.getEmail(),
                p.getTelefone(), p.getEspecialidade(),
                p.getDataNascimento(), p.getDataContratacao(),
                p.getStatus(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
