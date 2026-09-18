package br.com.alfaschool.backend.application.access.biometria;

/**
 * A face NAO pode sair do sistema para o equipamento.
 *
 * Levantada quando falta base legal declarada ou consentimento
 * registrado. Nao e' erro tecnico: e' o sistema se recusando a tratar
 * dado biometrico de crianca sem o que a LGPD exige (Art. 11 para dado
 * sensivel, Art. 14 para dado de crianca e adolescente).
 *
 * Bloquear na exportacao, e nao no cadastro, e' proposital: a escola
 * pode ter a foto no cadastro e ainda estar colhendo a autorizacao dos
 * pais. O que nao pode e' a foto chegar ao leitor antes disso.
 */
public class ExportacaoBiometriaBloqueadaException extends RuntimeException {

    private final String codigo;

    public ExportacaoBiometriaBloqueadaException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
