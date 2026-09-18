package br.com.alfaschool.backend.domain.access.autorizacao;

/**
 * Natureza da restricao. A coluna acc_restricoes.tipo e' VARCHAR(20) com
 * default 'JUDICIAL'.
 *
 * Nao existe enum equivalente em domain/access/shared/, por isso fica aqui.
 * O tipo nao muda o efeito: qualquer restricao ativa e vigente bloqueia.
 */
public enum TipoRestricao {
    /** Decisao judicial: guarda, medida protetiva, afastamento. */
    JUDICIAL,
    /** Decisao da propria escola: divergencia entre responsaveis, incidente. */
    ADMINISTRATIVA
}
