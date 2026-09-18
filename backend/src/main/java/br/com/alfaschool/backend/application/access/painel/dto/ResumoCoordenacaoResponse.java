package br.com.alfaschool.backend.application.access.painel.dto;

import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contadores da tela da coordenacao.
 *
 * Cada numero vem de uma consulta agregada propria. Contar em Java sobre a
 * lista inteira funcionaria com 30 alunos e travaria com 1200.
 */
public record ResumoCoordenacaoResponse(
        UUID unitId,
        Instant servidorEm,
        /** Alunos com permanencia aberta: dentro da escola agora. */
        long alunosPresentes,
        /** Retiradas em SOLICITADA, PREPARANDO ou PRONTO. */
        long aguardandoRetirada,
        /** Esperando ha mais tempo do que o limite configurado. */
        long horarioExcedido,
        /** Saidas efetivas (saida_em) registradas hoje. */
        long saidasConcluidas,
        int limiteEsperaMinutos,
        List<RetiradaFilaItem> fila
) {
}
