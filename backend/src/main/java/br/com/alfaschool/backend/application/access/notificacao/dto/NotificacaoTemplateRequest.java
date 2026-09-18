package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificacaoTemplateRequest(
        @NotNull EventoNotificacao evento,
        @NotNull CanalNotificacao canal,
        @Size(max = 255) String assunto,
        @NotBlank String corpo,
        @Size(max = 120) String templateExterno,
        Boolean ativo
) {
}
