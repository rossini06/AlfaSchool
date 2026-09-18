package br.com.alfaschool.backend.access.controlid;

import br.com.alfaschool.backend.application.access.controlid.ControlIdFotoErros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Traducao das recusas de cadastro facial.
 *
 * O motivo precisa chegar legivel a secretaria: "erro ao sincronizar" nao
 * diz a ninguem que bastava pedir para a crianca abrir os olhos.
 */
class ControlIdFotoErrosTest {

    @ParameterizedTest
    @CsvSource({
            "2,rosto",
            "5,distante",
            "6,próximo",
            "8,nitidez",
            "9,cortado"
    })
    @DisplayName("codigo numerico vira mensagem em portugues")
    void codigoNumerico(int code, String trecho) {
        ControlIdFotoErros.Recusa r = ControlIdFotoErros.traduzir(code, "whatever");

        assertThat(r.codigo()).isEqualTo(String.valueOf(code));
        assertThat(r.mensagem().toLowerCase()).contains(trecho.toLowerCase());
    }

    @Test
    @DisplayName("firmware antigo sem code: a mensagem em ingles ainda e' traduzida")
    void fallbackPorMensagem() {
        assertThat(ControlIdFotoErros.traduzir(null, "Face not detected").codigo()).isEqualTo("2");
        assertThat(ControlIdFotoErros.traduzir(null, "Face too distant").codigo()).isEqualTo("5");
        assertThat(ControlIdFotoErros.traduzir(null, "Low sharpness").codigo()).isEqualTo("8");
        assertThat(ControlIdFotoErros.traduzir(null, "Closed eyes detected").codigo()).isEqualTo("8");
    }

    @Test
    @DisplayName("'too close to image borders' e' rosto cortado, nao rosto proximo")
    void ordemDosPadroesImporta() {
        // Se "too close" fosse testado antes, o operador aproximaria ainda
        // mais a camera de um rosto que ja esta cortado na borda.
        assertThat(ControlIdFotoErros.traduzir(null, "Face too close to image borders").codigo())
                .isEqualTo("9");
        assertThat(ControlIdFotoErros.traduzir(null, "Face too close").codigo())
                .isEqualTo("6");
    }

    @Test
    @DisplayName("'not centered' com 'pose' e' cabeca inclinada")
    void poseInclinada() {
        assertThat(ControlIdFotoErros.traduzir(null, "Face not centered, bad pose").codigo())
                .isEqualTo("7");
    }

    @Test
    @DisplayName("mensagem desconhecida e' repassada inteira: nunca esconde o motivo")
    void mensagemDesconhecidaEhRepassada() {
        ControlIdFotoErros.Recusa r =
                ControlIdFotoErros.traduzir(null, "Sensor temperature out of range");

        assertThat(r.codigo()).isEqualTo(ControlIdFotoErros.CODIGO_DESCONHECIDO);
        assertThat(r.mensagem()).isEqualTo("Sensor temperature out of range");
    }

    @Test
    @DisplayName("recusa sem codigo nem mensagem ainda produz texto util")
    void recusaSemInformacao() {
        ControlIdFotoErros.Recusa r = ControlIdFotoErros.traduzir(null, null);

        assertThat(r.mensagem()).isNotBlank();
    }
}
