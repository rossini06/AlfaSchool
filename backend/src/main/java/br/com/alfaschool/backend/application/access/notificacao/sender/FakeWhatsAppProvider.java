package br.com.alfaschool.backend.application.access.notificacao.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provedor de mentira: registra a mensagem no log e devolve sucesso com um id
 * fake.
 *
 * <p>E' o que permite validar o fluxo inteiro de notificacao sem conta Meta
 * Business, sem template aprovado e sem custo por conversa. Ativo por padrao
 * (ver {@code matchIfMissing}); desligue em producao apontando
 * {@code app.access.notificacao.whatsapp.provider=META_CLOUD}.
 */
@Component
@ConditionalOnProperty(name = "app.access.notificacao.whatsapp.provider",
        havingValue = "FAKE", matchIfMissing = true)
public class FakeWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(FakeWhatsAppProvider.class);

    @Override
    public String nome() {
        return "FAKE";
    }

    @Override
    public ResultadoEnvio enviar(EnvioRequest request) {
        String destino = request.destino();
        if (destino == null || destino.replaceAll("\\D", "").length() < 10) {
            // Numero malformado nao melhora com retry.
            return ResultadoEnvio.permanente("NUMERO_INVALIDO", "Numero de WhatsApp invalido: " + destino);
        }
        String id = "fake-wa-" + UUID.randomUUID();
        log.info("[NOTIFICACAO][WHATSAPP][FAKE][{}] para={} template={} corpo={}",
                id, destino, request.templateExterno(), request.corpo());
        return ResultadoEnvio.ok(id);
    }
}
