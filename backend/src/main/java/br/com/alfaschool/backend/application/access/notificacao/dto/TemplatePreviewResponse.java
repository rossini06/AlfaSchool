package br.com.alfaschool.backend.application.access.notificacao.dto;

import java.util.Set;

/**
 * @param marcadores variaveis encontradas no template, para a tela mostrar o
 *                   que precisa ser preenchido.
 */
public record TemplatePreviewResponse(
        String assunto,
        String corpo,
        Set<String> marcadores,
        boolean usouTemplatePadrao
) {
}
