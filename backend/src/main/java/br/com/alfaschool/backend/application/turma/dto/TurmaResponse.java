package br.com.alfaschool.backend.application.turma.dto;

import br.com.alfaschool.backend.domain.turma.Turma;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TurmaResponse(
    UUID id, UUID tenantId, UUID unitId, UUID cursoId,
    String nome, String codigo, int anoLetivo, String turno,
    String professorResponsavel, int capacidadeMaxima,
    LocalDate dataInicio, LocalDate dataFim, String status,
    boolean ativa, Instant createdAt, Instant updatedAt
) {
    public static TurmaResponse from(Turma t) {
        return new TurmaResponse(t.getId(), t.getTenantId(), t.getUnitId(), t.getCursoId(),
            t.getNome(), t.getCodigo(), t.getAnoLetivo(), t.getTurno(),
            t.getProfessorResponsavel(), t.getCapacidadeMaxima(),
            t.getDataInicio(), t.getDataFim(), t.getStatus(),
            t.isAtiva(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
