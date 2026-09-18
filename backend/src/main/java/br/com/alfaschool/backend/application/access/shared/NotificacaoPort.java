package br.com.alfaschool.backend.application.access.shared;

import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;

import java.util.Map;
import java.util.UUID;

/**
 * Entrada unica do motor de notificacoes. Quem dispara nao escolhe canal
 * nem destinatario: isso vem das preferencias e do consentimento de cada
 * responsavel.
 *
 * Enfileirar nunca lanca excecao para o chamador — uma falha de e-mail nao
 * pode derrubar o registro de uma entrada na portaria.
 */
public interface NotificacaoPort {

    /**
     * @param chaveIdempotencia identificador estavel do fato (ex.:
     *        "ENTRADA:<eventoId>"). Garante que o reprocessamento da fila
     *        offline do agente nao avise a familia duas vezes.
     * @param variaveis valores do template. Nao incluir foto, biometria nem
     *        dado sensivel de crianca.
     */
    void enfileirar(UUID tenantId,
                    EventoNotificacao evento,
                    TitularTipo titularTipo,
                    UUID titularId,
                    UUID alunoId,
                    Map<String, String> variaveis,
                    String chaveIdempotencia);
}
