package br.com.alfaschool.backend.access.controlid;

import br.com.alfaschool.backend.application.access.controlid.AcionamentoAcesso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Whitelist de acionamento.
 *
 * O campo "parameters" e' texto livre no firmware. Sem esta whitelist,
 * qualquer um com acesso ao endpoint de abrir porta teria o console do
 * equipamento.
 */
class AcionamentoAcessoTest {

    @ParameterizedTest
    @CsvSource({
            "PORTA,door=1,door",
            "PORTA,door=2,door",
            "CATRACA_GIRO,allow=clockwise,catra",
            "CATRACA_GIRO,allow=anticlockwise,catra",
            "CATRACA_GIRO,allow=both,catra",
            "CATRACA_RELE,relay=1,catra",
            "CATRACA_RELE,relay=2,catra",
            "SEC_BOX,'id=3, reason=3',sec_box"
    })
    @DisplayName("comandos da whitelist passam com a action correta")
    void comandosPermitidos(String tipo, String parametros, String actionEsperada) {
        AcionamentoAcesso a = AcionamentoAcesso.de(tipo, parametros);

        assertThat(a.action()).isEqualTo(actionEsperada);
        assertThat(a.parametros()).isEqualTo(parametros);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "door=3",          // so' existem os portais 1 e 2
            "door=0",
            "door=1;reboot",   // tentativa de encadear comando
            "door=1 door=2",
            "id=1",            // a armadilha real: 200 no firmware, rele nao pulsa
            "DOOR=1",
            "door=",
            "door=1\n",
            ""
    })
    @DisplayName("parametro fora do padrao e' rejeitado antes de chegar ao equipamento")
    void parametrosInvalidosDePorta(String parametros) {
        assertThatThrownBy(() -> AcionamentoAcesso.de("PORTA", parametros))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"allow=sideways", "allow=CLOCKWISE", "allow=both,relay=1", "relay=3"})
    @DisplayName("giro de catraca fora dos tres sentidos conhecidos e' rejeitado")
    void sentidosInvalidosDeCatraca(String parametros) {
        assertThatThrownBy(() -> AcionamentoAcesso.de("CATRACA_GIRO", parametros))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reboot", "set_configuration", "door", "", "  "})
    @DisplayName("tipo de acionamento desconhecido nunca vira comando")
    void tipoForaDaWhitelist(String tipo) {
        assertThatThrownBy(() -> AcionamentoAcesso.de(tipo, "door=1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acionamento");
    }

    @Test
    @DisplayName("tipo nulo e' rejeitado")
    void tipoNulo() {
        assertThatThrownBy(() -> AcionamentoAcesso.de(null, "door=1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parametro nulo e' rejeitado no proprio record")
    void parametroNulo() {
        assertThatThrownBy(() ->
                new AcionamentoAcesso(AcionamentoAcesso.Tipo.PORTA, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sec_box exige o espaco depois da virgula, como o firmware")
    void secBoxExigeFormatoExato() {
        assertThat(AcionamentoAcesso.de("SEC_BOX", "id=1, reason=3").parametros())
                .isEqualTo("id=1, reason=3");
        assertThatThrownBy(() -> AcionamentoAcesso.de("SEC_BOX", "id=1,reason=3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AcionamentoAcesso.de("SEC_BOX", "id=1, reason=9"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("fabricas de conveniencia produzem comandos validos")
    void fabricas() {
        assertThat(AcionamentoAcesso.abrirPorta(1).parametros()).isEqualTo("door=1");
        assertThat(AcionamentoAcesso.liberarCatraca("both").parametros()).isEqualTo("allow=both");
        assertThat(AcionamentoAcesso.pulsarRele(2).parametros()).isEqualTo("relay=2");
        assertThatThrownBy(() -> AcionamentoAcesso.abrirPorta(7))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
