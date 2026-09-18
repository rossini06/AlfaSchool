package br.com.alfaschool.backend.access.biometria;

import br.com.alfaschool.backend.application.access.biometria.FotoUrlAssinada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A URL assinada e' o que impede que o link de uma foto de crianca vire
 * permanente e transferivel. Estes casos guardam essa propriedade.
 */
class FotoUrlAssinadaTest {

    private static final String SEGREDO = "chave-de-teste-com-mais-de-32-caracteres-para-hmac";

    private FotoUrlAssinada servico() {
        return new FotoUrlAssinada(SEGREDO, "/api/v1/access/fotos");
    }

    private static String param(String url, String nome) {
        for (String parte : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] kv = parte.split("=", 2);
            if (kv[0].equals(nome)) {
                return kv[1];
            }
        }
        return null;
    }

    @Test
    @DisplayName("URL recem-emitida e' aceita")
    void urlValida() {
        FotoUrlAssinada s = servico();
        String url = s.emitir("tenant/foto-1.jpg");

        assertThat(s.valida("tenant/foto-1.jpg", param(url, "exp"), param(url, "sig"))).isTrue();
    }

    @Test
    @DisplayName("assinatura de uma foto nao serve para outra")
    void assinaturaNaoTransferePraOutraFoto() {
        FotoUrlAssinada s = servico();
        String url = s.emitir("tenant/aluno-a.jpg");

        // E' o ataque obvio: trocar a chave na barra de enderecos para ver
        // a foto de outra crianca reaproveitando a mesma assinatura.
        assertThat(s.valida("tenant/aluno-b.jpg", param(url, "exp"), param(url, "sig"))).isFalse();
    }

    @Test
    @DisplayName("URL vencida e' recusada")
    void urlVencida() {
        FotoUrlAssinada s = servico();
        String url = s.emitir("tenant/foto-1.jpg");
        String vencido = String.valueOf(Instant.now().getEpochSecond() - 1);

        // Mesmo com assinatura autentica para aquele exp, o prazo manda.
        assertThat(s.valida("tenant/foto-1.jpg", vencido, param(url, "sig"))).isFalse();
    }

    @Test
    @DisplayName("assinatura adulterada e' recusada")
    void assinaturaAdulterada() {
        FotoUrlAssinada s = servico();
        String url = s.emitir("tenant/foto-1.jpg");

        assertThat(s.valida("tenant/foto-1.jpg", param(url, "exp"), "AAAA" + param(url, "sig"))).isFalse();
    }

    @Test
    @DisplayName("faltando qualquer parte, recusa — falha fechada")
    void faltandoParte() {
        FotoUrlAssinada s = servico();
        String url = s.emitir("tenant/foto-1.jpg");

        assertThat(s.valida("tenant/foto-1.jpg", null, param(url, "sig"))).isFalse();
        assertThat(s.valida("tenant/foto-1.jpg", param(url, "exp"), null)).isFalse();
        assertThat(s.valida(null, param(url, "exp"), param(url, "sig"))).isFalse();
        assertThat(s.valida("tenant/foto-1.jpg", "nao-e-numero", param(url, "sig"))).isFalse();
    }

    @Test
    @DisplayName("chave nula nao gera URL")
    void semFotoNaoGeraUrl() {
        assertThat(servico().emitir(null)).isNull();
        assertThat(servico().emitir("  ")).isNull();
    }

    @Test
    @DisplayName("segredo curto derruba a aplicacao no boot, nao em producao")
    void segredoCurtoFalhaNoBoot() {
        assertThatThrownBy(() -> new FotoUrlAssinada("curta", "/api/v1/access/fotos"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("segredos diferentes nao validam a assinatura um do outro")
    void segredoDiferenteNaoValida() {
        FotoUrlAssinada a = servico();
        FotoUrlAssinada b = new FotoUrlAssinada("outra-chave-de-teste-com-mais-de-32-caracteres", "/api/v1/access/fotos");
        String url = a.emitir("tenant/foto-1.jpg");

        assertThat(b.valida("tenant/foto-1.jpg", param(url, "exp"), param(url, "sig"))).isFalse();
    }
}
