package br.com.alfaschool.backend.application.access.notificacao.sender;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;

import java.util.Map;
import java.util.UUID;

/**
 * Tudo que um canal precisa para entregar uma mensagem ja renderizada.
 *
 * <p>O corpo chega pronto: renderizar no momento de enfileirar (e nao no de
 * enviar) garante que o historico guarde exatamente o texto que a familia leu,
 * mesmo que o template mude depois.
 *
 * @param templateExterno nome do template aprovado no provedor, quando o canal
 *                        exige (WhatsApp).
 * @param variaveis       mantidas para canais de template externo, que exigem
 *                        os componentes separados do texto final. Ja passaram
 *                        pela validacao anti-foto/biometria.
 * @param config          configuracao do canal, com o segredo ainda cifrado.
 */
public record EnvioRequest(
        UUID envioId,
        UUID tenantId,
        String destino,
        String assunto,
        String corpo,
        String templateExterno,
        Map<String, String> variaveis,
        AccNotificacaoConfig config
) {
}
