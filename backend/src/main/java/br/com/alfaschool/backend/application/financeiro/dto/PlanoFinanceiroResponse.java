package br.com.alfaschool.backend.application.financeiro.dto;

import br.com.alfaschool.backend.domain.financeiro.PlanoFinanceiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanoFinanceiroResponse(
        UUID id, UUID tenantId,
        String nome, BigDecimal valor, String periodicidade,
        String descricao, boolean ativo,
        Instant createdAt, Instant updatedAt
) {
    public static PlanoFinanceiroResponse from(PlanoFinanceiro p) {
        return new PlanoFinanceiroResponse(
                p.getId(), p.getTenantId(),
                p.getNome(), p.getValor(), p.getPeriodicidade(),
                p.getDescricao(), p.isAtivo(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
