package br.com.alfaschool.backend.application.access.biometria;

import java.util.UUID;

/**
 * Armazenamento da foto de referencia.
 *
 * A implementacao de hoje grava em disco local. A interface existe para
 * que trocar por S3/MinIO seja escrever outra classe, e nao caçar
 * File.open espalhado por services — foto de crianca e' dado sensivel e
 * o ponto de acesso a ela precisa ser um so'.
 *
 * A chave devolvida e' OPACA: quem chama guarda a string e nao supoe que
 * ela seja um caminho de arquivo.
 */
public interface FotoStorage {

    /**
     * Valida e grava os bytes.
     *
     * @return chave opaca para recuperar a foto depois
     * @throws IllegalArgumentException se nao for JPEG/PNG valido ou
     *         exceder o tamanho maximo
     */
    String salvar(UUID tenantId, byte[] conteudo);

    /** @throws IllegalStateException se a chave nao existir mais */
    byte[] ler(String chave);

    void remover(String chave);

    boolean existe(String chave);
}
