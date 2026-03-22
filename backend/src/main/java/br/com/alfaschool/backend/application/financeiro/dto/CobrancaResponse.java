package br.com.alfaschool.backend.application.financeiro.dto;

import br.com.alfaschool.backend.domain.financeiro.Cobranca;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CobrancaResponse(
        UUID id, UUID tenantId,
        UUID contratoId, UUID alunoId,
        BigDecimal valor, String descricao,
        LocalDate vencimento, LocalDate dataPagamento,
        String status, String competencia,
        Instant createdAt, Instant updatedAt
) {
    public static CobrancaResponse from(Cobranca c) {
        return new CobrancaResponse(
                c.getId(), c.getTenantId(),
                c.getContratoId(), c.getAlunoId(),
                c.getValor(), c.getDescricao(),
                c.getVencimento(), c.getDataPagamento(),
                c.getStatus(), c.getCompetencia(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
