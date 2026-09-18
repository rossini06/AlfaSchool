package br.com.alfaschool.backend.application.access.controlid;

/**
 * O equipamento recusou a foto.
 *
 * Recusa e' DEFINITIVA para a mesma imagem: reenviar os mesmos bytes
 * produz a mesma recusa e so' gasta ciclo. Quem trata deve pedir outra
 * foto, nao repetir a tentativa.
 *
 * O motivo vem estruturado (codigo + mensagem em portugues) porque nos
 * sistemas anteriores ele se perdia num validate generico e a secretaria
 * ficava com "erro ao sincronizar" sem saber que bastava abrir os olhos.
 */
public class FotoRecusadaException extends RuntimeException {

    private final String codigo;

    public FotoRecusadaException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
