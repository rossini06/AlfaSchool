package br.com.alfaschool.backend.access.retirada;

import br.com.alfaschool.backend.domain.access.retirada.MaquinaEstadosRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaquinaEstadosRetiradaTest {

    @ParameterizedTest(name = "{0} -> {1} e valida")
    @CsvSource({
            "SOLICITADA,PREPARANDO",
            "SOLICITADA,PRONTO",
            "SOLICITADA,ENTREGUE",
            "SOLICITADA,CANCELADA",
            "SOLICITADA,NEGADA",
            "PREPARANDO,PRONTO",
            "PREPARANDO,ENTREGUE",
            "PREPARANDO,CANCELADA",
            "PREPARANDO,NEGADA",
            "PRONTO,ENTREGUE",
            "PRONTO,CANCELADA",
            "PRONTO,NEGADA"
    })
    void transicoesValidas(StatusRetirada de, StatusRetirada para) {
        assertTrue(MaquinaEstadosRetirada.permitida(de, para));
        assertNull(MaquinaEstadosRetirada.validarTransicao(de, para));
    }

    @ParameterizedTest(name = "{0} -> {1} e invalida")
    @CsvSource({
            // Estados finais nao voltam atras: desfazer uma entrega seria
            // reescrever um ato de responsabilidade ja praticado.
            "ENTREGUE,PREPARANDO",
            "ENTREGUE,PRONTO",
            "ENTREGUE,CANCELADA",
            "ENTREGUE,NEGADA",
            "ENTREGUE,SOLICITADA",
            "CANCELADA,PREPARANDO",
            "CANCELADA,ENTREGUE",
            "CANCELADA,SOLICITADA",
            "NEGADA,ENTREGUE",
            "NEGADA,PRONTO",
            "NEGADA,SOLICITADA",
            // Nao se volta para tras no fluxo feliz
            "PREPARANDO,SOLICITADA",
            "PRONTO,SOLICITADA",
            "PRONTO,PREPARANDO"
    })
    void transicoesInvalidas(StatusRetirada de, StatusRetirada para) {
        assertFalse(MaquinaEstadosRetirada.permitida(de, para));
        assertNotNull(MaquinaEstadosRetirada.validarTransicao(de, para));
    }

    @Test
    @DisplayName("Repetir o mesmo status devolve mensagem clara, nao silencio")
    void mesmoStatusNaoEhTransicao() {
        for (StatusRetirada status : StatusRetirada.values()) {
            assertNotNull(MaquinaEstadosRetirada.validarTransicao(status, status));
        }
    }

    @Test
    @DisplayName("Mensagem de retirada ja entregue e a que a portaria le")
    void mensagemDeEntregue() {
        String erro = MaquinaEstadosRetirada.validarTransicao(StatusRetirada.ENTREGUE, StatusRetirada.PRONTO);
        assertNotNull(erro);
        assertTrue(erro.toLowerCase().contains("entregue"), "mensagem foi: " + erro);
    }

    @Test
    void estadosFinais() {
        assertTrue(MaquinaEstadosRetirada.ehFinal(StatusRetirada.ENTREGUE));
        assertTrue(MaquinaEstadosRetirada.ehFinal(StatusRetirada.CANCELADA));
        assertTrue(MaquinaEstadosRetirada.ehFinal(StatusRetirada.NEGADA));
        assertFalse(MaquinaEstadosRetirada.ehFinal(StatusRetirada.SOLICITADA));
        assertFalse(MaquinaEstadosRetirada.ehFinal(StatusRetirada.PREPARANDO));
        assertFalse(MaquinaEstadosRetirada.ehFinal(StatusRetirada.PRONTO));
    }

    @Test
    void transicaoComNuloNaoPassa() {
        assertFalse(MaquinaEstadosRetirada.permitida(null, StatusRetirada.ENTREGUE));
        assertFalse(MaquinaEstadosRetirada.permitida(StatusRetirada.SOLICITADA, null));
        assertNotNull(MaquinaEstadosRetirada.validarTransicao(null, null));
    }
}
