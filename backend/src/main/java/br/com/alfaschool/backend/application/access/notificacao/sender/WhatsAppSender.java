package br.com.alfaschool.backend.application.access.notificacao.sender;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Canal de WhatsApp. Nao fala com provedor nenhum: delega para o
 * {@link WhatsAppProvider} ativo.
 *
 * <p>A escolha do provedor e' por configuracao
 * ({@code app.access.notificacao.whatsapp.provider}), nunca por {@code if} no
 * meio do fluxo. Se a config do tenant nomear um provider especifico em
 * {@code acc_notificacao_configs.provider}, ele tem precedencia — permite que
 * uma escola piloto rode no FAKE enquanto as outras ja estao na Meta.
 */
@Component
public class WhatsAppSender implements NotificacaoSender {

    private final ObjectProvider<WhatsAppProvider> providers;

    public WhatsAppSender(ObjectProvider<WhatsAppProvider> providers) {
        this.providers = providers;
    }

    @Override
    public CanalNotificacao canal() {
        return CanalNotificacao.WHATSAPP;
    }

    @Override
    public ResultadoEnvio enviar(EnvioRequest request) {
        List<WhatsAppProvider> disponiveis = providers.stream().toList();
        if (disponiveis.isEmpty()) {
            // Sem provedor nao adianta reagendar: o operador tem de configurar.
            return ResultadoEnvio.permanente("SEM_PROVIDER_WHATSAPP",
                    "Nenhum WhatsAppProvider ativo. Configure app.access.notificacao.whatsapp.provider");
        }

        String desejado = request.config() != null ? request.config().getProvider() : null;
        WhatsAppProvider escolhido = disponiveis.stream()
                .filter(p -> desejado != null && p.nome().equalsIgnoreCase(desejado))
                .findFirst()
                .orElse(disponiveis.get(0));

        try {
            return escolhido.enviar(request);
        } catch (RuntimeException e) {
            // Contrato: sender nunca lanca. Desconhecido vira transitorio.
            return ResultadoEnvio.transitorio("ERRO_PROVIDER", e.toString());
        }
    }
}
