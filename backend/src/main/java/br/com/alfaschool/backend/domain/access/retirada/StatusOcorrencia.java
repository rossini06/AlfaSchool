package br.com.alfaschool.backend.domain.access.retirada;

/**
 * Fluxo de tratamento: ABERTA -> EM_TRATATIVA -> FECHADA.
 *
 * Fechar exige que alguem tenha assumido a tratativa; ocorrencia que some
 * da tela sem ninguem ter olhado e' exatamente o que a coordenacao nao
 * pode ter.
 */
public enum StatusOcorrencia {
    ABERTA, EM_TRATATIVA, FECHADA
}
