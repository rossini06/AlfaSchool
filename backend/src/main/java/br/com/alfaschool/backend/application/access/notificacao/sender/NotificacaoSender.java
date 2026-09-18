package br.com.alfaschool.backend.application.access.notificacao.sender;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;

/**
 * Canal de saida plugavel.
 *
 * <p>O nome NAO e' "CanalNotificacao" de proposito: ja existe um enum com esse
 * nome em {@code domain/access/shared}, e o enum e' o vocabulario do dominio.
 * Esta interface e' o mecanismo.
 *
 * <p>Um sender NUNCA lanca: toda falha vira {@link ResultadoEnvio} classificado
 * como permanente ou transitorio, porque e' essa classificacao que decide se a
 * linha volta para a fila.
 */
public interface NotificacaoSender {

    CanalNotificacao canal();

    ResultadoEnvio enviar(EnvioRequest request);
}
