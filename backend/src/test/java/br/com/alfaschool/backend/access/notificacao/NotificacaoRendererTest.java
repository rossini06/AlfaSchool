package br.com.alfaschool.backend.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.NotificacaoRenderer;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacaoRendererTest {

    private final NotificacaoRenderer renderer = new NotificacaoRenderer();

    @Test
    @DisplayName("data-URI em qualquer variavel e' rejeitado")
    void rejeitaDataUri() {
        assertThatThrownBy(() -> renderer.validarVariaveis(Map.of(
                "observacao", "data:image/jpeg;base64,/9j/4AAQSkZJRg==")))
                .isInstanceOf(NotificacaoRenderer.VariavelProibidaException.class)
                .hasMessageContaining("data-URI");
    }

    @Test
    @DisplayName("base64 longo (foto ou template biometrico) e' rejeitado mesmo em campo de nome inocente")
    void rejeitaBase64Longo() {
        String base64 = "iVBORw0KGgoAAAANSUhEUgAA".repeat(20);
        assertThatThrownBy(() -> renderer.validarVariaveis(Map.of("detalhe", base64)))
                .isInstanceOf(NotificacaoRenderer.VariavelProibidaException.class)
                .hasMessageContaining("base64");
    }

    @Test
    @DisplayName("nomes de variavel sensiveis sao rejeitados mesmo com valor curto")
    void rejeitaNomesSensiveis() {
        for (String nome : new String[]{"foto", "biometria", "digital", "face", "observacoes_medicas", "cpf_aluno"}) {
            Map<String, String> v = new HashMap<>();
            v.put(nome, "x");
            assertThatThrownBy(() -> renderer.validarVariaveis(v))
                    .as("variavel %s deveria ser proibida", nome)
                    .isInstanceOf(NotificacaoRenderer.VariavelProibidaException.class);
        }
    }

    @Test
    @DisplayName("valor muito longo e' rejeitado: aviso nao e' dossie")
    void rejeitaValorLongo() {
        assertThatThrownBy(() -> renderer.validarVariaveis(Map.of("motivo", "a b ".repeat(200))))
                .isInstanceOf(NotificacaoRenderer.VariavelProibidaException.class);
    }

    @Test
    @DisplayName("variaveis normais de um aviso de acesso passam")
    void aceitaVariaveisNormais() {
        assertThatCode(() -> renderer.validarVariaveis(Map.of(
                "aluno", "Maria Silva",
                "hora", "07:42",
                "pessoa", "João Souza")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("substitui marcadores e nao deixa chave solta no texto")
    void renderiza() {
        String texto = renderer.renderizar("{aluno} entrou as {hora} em {unidade}. {inexistente}",
                Map.of("aluno", "Maria", "hora", "07:42", "unidade", "Centro"));
        assertThat(texto).isEqualTo("Maria entrou as 07:42 em Centro. ");
    }

    @Test
    @DisplayName("marcador com nome em maiusculas tambem e' substituido")
    void renderizaSemDiferenciarCaixa() {
        assertThat(renderer.renderizar("{ALUNO}", Map.of("aluno", "Maria"))).isEqualTo("Maria");
    }

    @Test
    @DisplayName("existe template de fabrica para todo evento e ele nao carrega dado sensivel")
    void templatesPadraoExistemESaoSeguros() {
        for (EventoNotificacao evento : EventoNotificacao.values()) {
            NotificacaoRenderer.TemplatePadrao padrao = renderer.padrao(evento, CanalNotificacao.EMAIL);
            assertThat(padrao.corpo()).as("corpo de %s", evento).isNotBlank();

            Map<String, String> marcadores = new HashMap<>(renderer.extrairMarcadores(padrao.corpo()));
            marcadores.putAll(renderer.extrairMarcadores(padrao.assunto()));
            assertThatCode(() -> renderer.validarVariaveis(marcadores))
                    .as("template padrao de %s nao pode pedir dado sensivel", evento)
                    .doesNotThrowAnyException();
        }
    }
}
