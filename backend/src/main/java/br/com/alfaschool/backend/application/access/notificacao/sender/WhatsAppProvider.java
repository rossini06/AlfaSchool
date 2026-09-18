package br.com.alfaschool.backend.application.access.notificacao.sender;

/**
 * Provedor concreto de WhatsApp.
 *
 * <p>O canal de WhatsApp e' abstrato POR DESIGN. Contratar provedor (Meta
 * Business verificada, template aprovado, cobranca por conversa) leva semanas;
 * o produto nao pode ficar parado esperando. Com esta interface o fluxo
 * completo — fila, claim atomico, backoff, teto diario, historico — roda hoje
 * contra o {@link FakeWhatsAppProvider} e amanha contra a Meta, trocando so' a
 * propriedade de configuracao.
 */
public interface WhatsAppProvider {

    /** Identificador usado em {@code acc_notificacao_configs.provider}. */
    String nome();

    ResultadoEnvio enviar(EnvioRequest request);
}
