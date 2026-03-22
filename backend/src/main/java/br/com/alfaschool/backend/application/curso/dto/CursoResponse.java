package br.com.alfaschool.backend.application.curso.dto;

import br.com.alfaschool.backend.domain.curso.Curso;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CursoResponse(
    UUID id, UUID tenantId, UUID unitId, String nome, String codigo,
    String descricao, Integer cargaHoraria, String modalidade, String nivel,
    String tipo, Integer duracaoMeses, Integer idadeMinima, Integer idadeMaxima,
    BigDecimal precoBase, BigDecimal notaMinimaAprovacao, BigDecimal frequenciaMinimaAprovacao,
    boolean ativo, Instant createdAt, Instant updatedAt
) {
    public static CursoResponse from(Curso c) {
        return new CursoResponse(c.getId(), c.getTenantId(), c.getUnitId(),
            c.getNome(), c.getCodigo(), c.getDescricao(), c.getCargaHoraria(),
            c.getModalidade(), c.getNivel(), c.getTipo(), c.getDuracaoMeses(),
            c.getIdadeMinima(), c.getIdadeMaxima(), c.getPrecoBase(),
            c.getNotaMinimaAprovacao(), c.getFrequenciaMinimaAprovacao(),
            c.isAtivo(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
