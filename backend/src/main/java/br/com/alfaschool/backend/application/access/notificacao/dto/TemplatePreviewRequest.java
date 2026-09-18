package br.com.alfaschool.backend.application.access.notificacao.dto;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;

import java.util.Map;

/**
 * Preview de template. Aceita corpo/assunto avulsos (para a tela editar sem
 * salvar) ou o par evento+canal (para renderizar o que esta gravado).
 */
public record TemplatePreviewRequest(
        EventoNotificacao evento,
        CanalNotificacao canal,
        String assunto,
        String corpo,
        Map<String, String> variaveis
) {
}
