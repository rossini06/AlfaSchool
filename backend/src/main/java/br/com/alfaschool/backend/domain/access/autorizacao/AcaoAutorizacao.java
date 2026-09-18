package br.com.alfaschool.backend.domain.access.autorizacao;

/**
 * Acoes registradas na trilha imutavel acc_autorizacao_historico.
 *
 * Nao existe enum equivalente em domain/access/shared/, por isso fica aqui,
 * no pacote que e' dono da tabela. A coluna e' VARCHAR(30).
 */
public enum AcaoAutorizacao {
    CRIACAO,
    APROVACAO,
    SUSPENSAO,
    REATIVACAO,
    REVOGACAO,
    EXPIRACAO
}
