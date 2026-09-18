package br.com.alfaschool.backend.application.access.simulador.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Cenarios do gerador de eventos. */
public final class SimuladorDtos {

    private SimuladorDtos() {
    }

    public record EntradaAlunoRequest(
            @NotNull(message = "alunoId é obrigatório.") UUID alunoId,
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId,
            Instant dataHora) {
    }

    public record ChegadaResponsavelRequest(
            @NotNull(message = "pessoaAutorizadaId é obrigatório.") UUID pessoaAutorizadaId,
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId,
            Instant dataHora) {
    }

    public record SaidaAlunoRequest(
            @NotNull(message = "alunoId é obrigatório.") UUID alunoId,
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId,
            Instant dataHora) {
    }

    public record AcessoNegadoRequest(
            @NotNull(message = "deviceUserId é obrigatório.") Long deviceUserId,
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId,
            String motivo) {
    }

    public record PessoaDesconhecidaRequest(
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId) {
    }

    public record EventoDuplicadoRequest(
            @NotNull(message = "eventoId é obrigatório.") UUID eventoId) {
    }

    public record RotinaDiaRequest(
            @NotNull(message = "unitId é obrigatório.") UUID unitId,
            Integer maximoAlunos) {
    }

    public record EventoGeradoDto(UUID eventoId, boolean replay, Long deviceLogId,
                                  Long deviceUserId, Instant dataHora, String cenario) {
    }

    public record RotinaDiaDto(int entradas, int chegadasDeResponsavel, int saidas,
                               List<EventoGeradoDto> eventos, List<String> avisos) {
    }
}
