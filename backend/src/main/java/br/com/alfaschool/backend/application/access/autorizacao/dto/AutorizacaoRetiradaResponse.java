package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.shared.OrigemAutorizacao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * documentoKey NAO entra aqui de proposito: a procuracao vive em storage
 * restrito e so sai pelo endpoint dedicado com @PreAuthorize mais estrito.
 */
public record AutorizacaoRetiradaResponse(
        UUID id,
        UUID tenantId,
        UUID alunoId,
        UUID pessoaAutorizadaId,
        boolean permanente,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        String diasSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        StatusAutorizacao status,
        OrigemAutorizacao origem,
        String motivo,
        UUID aprovadoPorUserId,
        Instant aprovadoEm,
        String observacao,
        Instant createdAt,
        Instant updatedAt
) {
    public static AutorizacaoRetiradaResponse from(AutorizacaoRetirada a) {
        return new AutorizacaoRetiradaResponse(
                a.getId(), a.getTenantId(), a.getAlunoId(), a.getPessoaAutorizadaId(),
                a.isPermanente(), a.getVigenciaInicio(), a.getVigenciaFim(),
                a.getDiasSemana(), a.getHoraInicio(), a.getHoraFim(),
                a.getStatus(), a.getOrigem(), a.getMotivo(),
                a.getAprovadoPorUserId(), a.getAprovadoEm(), a.getObservacao(),
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
