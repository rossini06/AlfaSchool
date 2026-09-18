package br.com.alfaschool.backend.application.access.retirada.dto;

import br.com.alfaschool.backend.domain.access.retirada.AccRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;

import java.time.Instant;
import java.util.UUID;

/** Retorno das operacoes de escrita. A tela da fila usa RetiradaFilaItem. */
public record RetiradaResponse(
        UUID id,
        UUID unitId,
        UUID alunoId,
        UUID turmaId,
        UUID salaId,
        UUID portariaId,
        UUID pessoaAutorizadaId,
        StatusRetirada status,
        Integer ordemChegada,
        Instant solicitadoEm,
        Instant preparandoEm,
        Instant prontoEm,
        Instant entregueEm,
        UUID entreguePorUserId,
        Instant saidaEm,
        boolean retiradaManual,
        String motivo,
        String observacao,
        long tempoEsperaMinutos
) {
    public static RetiradaResponse from(AccRetirada r) {
        return new RetiradaResponse(
                r.getId(),
                r.getUnitId(),
                r.getAlunoId(),
                r.getTurmaId(),
                r.getSalaId(),
                r.getPortariaId(),
                r.getPessoaAutorizadaId(),
                r.getStatus(),
                r.getOrdemChegada(),
                r.getSolicitadoEm(),
                r.getPreparandoEm(),
                r.getProntoEm(),
                r.getEntregueEm(),
                r.getEntreguePorUserId(),
                r.getSaidaEm(),
                r.isRetiradaManual(),
                r.getMotivo(),
                r.getObservacao(),
                r.tempoEsperaMinutos(Instant.now()));
    }
}
