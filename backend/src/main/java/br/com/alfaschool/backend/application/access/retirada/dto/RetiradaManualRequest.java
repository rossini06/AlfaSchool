package br.com.alfaschool.backend.application.access.retirada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Coordenacao abrindo retirada sem leitura biometrica.
 *
 * O motivo e' obrigatorio porque toda retirada manual gera ocorrencia de
 * auditoria: e' o caminho que contorna o controle, e contornar o controle
 * sem justificativa e' o que se quer impedir.
 */
public record RetiradaManualRequest(
        @NotNull(message = "Informe o aluno") UUID alunoId,
        UUID unitId,
        UUID portariaId,
        /** Quando quem retira ja' esta' cadastrado, vem o id; senao, so' o nome. */
        UUID pessoaAutorizadaId,
        String retiradoPorNome,
        String retiradoPorDocumento,
        @NotBlank(message = "Informe o motivo da retirada manual") String motivo,
        String observacao
) {
}
