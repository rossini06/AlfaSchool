package br.com.alfaschool.backend.application.access.autorizacao;

/**
 * Normalizacao e validacao de CPF.
 *
 * A normalizacao importa tanto quanto a validacao: a restricao judicial pode
 * chegar com "123.456.789-09" e o cadastro com "12345678909". Se os dois nao
 * forem comparados so por digitos, a restricao simplesmente nao casa e a
 * pessoa impedida passa. Por isso tudo e' persistido normalizado.
 *
 * Nao existe util de CPF no projeto; fica no pacote que precisa dela.
 */
public final class CpfUtils {

    private CpfUtils() {
    }

    /** Devolve so os digitos, ou null se nao sobrar nada. */
    public static String normalizar(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digitos = cpf.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    /**
     * Valida os dois digitos verificadores. Rejeita tamanho diferente de 11 e
     * as sequencias repetidas ("11111111111"), que passam no calculo mas nao
     * sao CPF de ninguem.
     */
    public static boolean valido(String cpf) {
        String d = normalizar(cpf);
        if (d == null || d.length() != 11) {
            return false;
        }
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = digitoVerificador(d, 9, 10);
        if (primeiro != d.charAt(9) - '0') {
            return false;
        }
        int segundo = digitoVerificador(d, 10, 11);
        return segundo == d.charAt(10) - '0';
    }

    private static int digitoVerificador(String digitos, int quantidade, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
