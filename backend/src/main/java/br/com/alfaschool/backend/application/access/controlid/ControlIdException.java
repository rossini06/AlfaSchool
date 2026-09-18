package br.com.alfaschool.backend.application.access.controlid;

/**
 * Falha de comunicacao ou de comando com o equipamento Control iD.
 *
 * Existe separada de RuntimeException para que revogacao de acesso possa
 * ser propagada sem ambiguidade: reportar "revogado" quando o leitor
 * recusou o comando e' inaceitavel numa escola.
 */
public class ControlIdException extends RuntimeException {

    private final int httpStatus;

    public ControlIdException(String mensagem) {
        this(mensagem, 0, null);
    }

    public ControlIdException(String mensagem, Throwable causa) {
        this(mensagem, 0, causa);
    }

    public ControlIdException(String mensagem, int httpStatus, Throwable causa) {
        super(mensagem, causa);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** Sessao expirada ou credencial trocada: vale refazer login uma vez. */
    public boolean sessaoInvalida() {
        return httpStatus == 401 || httpStatus == 403;
    }
}
