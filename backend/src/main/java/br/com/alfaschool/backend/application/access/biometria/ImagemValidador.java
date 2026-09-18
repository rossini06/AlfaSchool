package br.com.alfaschool.backend.application.access.biometria;

/**
 * Validacao do arquivo de foto por MAGIC BYTES, nao por extensao nem por
 * Content-Type.
 *
 * Extensao e header sao escolhidos por quem envia. Um .jpg que na verdade
 * e' um SVG com script, ou um ZIP, seria aceito por qualquer checagem de
 * nome — e depois servido de volta para o navegador da secretaria.
 * Os primeiros bytes do arquivo sao a unica parte que o formato controla.
 */
public final class ImagemValidador {

    private ImagemValidador() {
    }

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public enum Formato { JPEG, PNG }

    /**
     * @throws IllegalArgumentException com mensagem em portugues quando o
     *         arquivo nao serve
     */
    public static Formato validar(byte[] conteudo, long tamanhoMaximo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("Arquivo de foto vazio.");
        }
        if (conteudo.length > tamanhoMaximo) {
            throw new IllegalArgumentException("Foto maior que o limite de "
                    + (tamanhoMaximo / 1024) + " KB.");
        }
        // Menor que isso nao cabe nem o cabecalho: e' lixo ou truncado.
        if (conteudo.length < 64) {
            throw new IllegalArgumentException("Arquivo de foto truncado ou inválido.");
        }
        if (comecaCom(conteudo, JPEG)) {
            return Formato.JPEG;
        }
        if (comecaCom(conteudo, PNG)) {
            return Formato.PNG;
        }
        throw new IllegalArgumentException("Formato de foto não suportado. Envie JPEG ou PNG.");
    }

    public static boolean ehJpeg(byte[] conteudo) {
        return conteudo != null && comecaCom(conteudo, JPEG);
    }

    private static boolean comecaCom(byte[] conteudo, byte[] assinatura) {
        if (conteudo.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
