package br.com.alfaschool.backend.application.matricula.dto;

import br.com.alfaschool.backend.domain.matricula.Matricula;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MatriculaResponse(
    UUID id, UUID tenantId, UUID unitId, UUID alunoId, UUID turmaId,
    String numeroMatricula, LocalDate dataMatricula, LocalDate dataConclusao,
    String status, String obs, String tipo, String statusAcademico, BigDecimal desconto,
    Instant createdAt, Instant updatedAt
) {
    public static MatriculaResponse from(Matricula m) {
        return new MatriculaResponse(
            m.getId(), m.getTenantId(), m.getUnitId(), m.getAlunoId(), m.getTurmaId(),
            m.getNumeroMatricula(), m.getDataMatricula(), m.getDataConclusao(),
            m.getStatus(), m.getObs(), m.getTipo(), m.getStatusAcademico(), m.getDesconto(),
            m.getCreatedAt(), m.getUpdatedAt()
        );
    }
}
