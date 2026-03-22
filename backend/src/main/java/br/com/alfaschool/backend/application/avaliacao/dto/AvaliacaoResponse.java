package br.com.alfaschool.backend.application.avaliacao.dto;

import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoResponse(
        UUID id,
        UUID tenantId,
        UUID turmaId,
        UUID disciplinaId,
        String nome,
        String tipo,
        BigDecimal peso,
        LocalDate dataAvaliacao,
        BigDecimal notaMaxima,
        String periodo,
        BigDecimal notaMinima,
        String status,
        String descricao,
        String criterios,
        LocalDate dataEntrega,
        Boolean permiteRecuperacao,
        Instant createdAt,
        Instant updatedAt
) {
    public static AvaliacaoResponse from(Avaliacao a) {
        return new AvaliacaoResponse(
                a.getId(),
                a.getTenantId(),
                a.getTurmaId(),
                a.getDisciplinaId(),
                a.getNome(),
                a.getTipo(),
                a.getPeso(),
                a.getDataAvaliacao(),
                a.getNotaMaxima(),
                a.getPeriodo(),
                a.getNotaMinima(),
                a.getStatus(),
                a.getDescricao(),
                a.getCriterios(),
                a.getDataEntrega(),
                a.getPermiteRecuperacao(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
