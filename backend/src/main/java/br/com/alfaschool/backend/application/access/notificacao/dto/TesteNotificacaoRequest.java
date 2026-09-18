package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Disparo de teste para um destino informado pelo operador.
 *
 * <p>NAO passa pelo motor de preferencias de proposito: e' o operador testando
 * a propria infraestrutura, com o proprio contato. Nao ha dado de aluno
 * envolvido, entao nao ha consentimento de terceiro a respeitar.
 */
public record TesteNotificacaoRequest(
        @NotNull CanalNotificacao canal,
        @NotBlank @Size(max = 160) String destino,
        @Size(max = 200) String assunto,
        @Size(max = 1000) String mensagem
) {
}
