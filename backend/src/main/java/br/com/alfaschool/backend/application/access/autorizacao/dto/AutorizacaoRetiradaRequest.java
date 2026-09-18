package br.com.alfaschool.backend.application.access.autorizacao.dto;

import br.com.alfaschool.backend.domain.access.shared.OrigemAutorizacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * diasSemana em CSV ISO-8601: 1=segunda ... 7=domingo (ex.: "1,3,5").
 * Vazio = todos os dias.
 *
 * origem PORTAL ignora qualquer tentativa de nascer ATIVA: o service forca
 * PENDENTE. Solicitacao de responsavel jamais libera sozinha.
 */
public record AutorizacaoRetiradaRequest(
        @NotNull UUID alunoId,
        @NotNull UUID pessoaAutorizadaId,
        @NotNull Boolean permanente,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        @Size(max = 20) String diasSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        OrigemAutorizacao origem,
        @Size(max = 255) String motivo,
        @Size(max = 255) String documentoKey,
        String observacao
) {
}
