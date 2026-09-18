package br.com.alfaschool.backend.domain.access.retirada;

/**
 * De onde partiu a mudanca de status. Vira coluna `origem` em
 * acc_retirada_historico.
 *
 * Nao existe em domain/access/shared porque nenhuma outra fatia usa: e'
 * vocabulario da trilha de auditoria da fila de retirada.
 */
public enum OrigemTransicao {
    /** Leitura biometrica na portaria abriu a retirada sozinha. */
    CATRACA,
    /** Professora ou coordenacao tocou no painel/app. */
    PAINEL,
    /** Coordenacao usou a tela administrativa (inclui retirada manual). */
    ADMIN,
    /** Rotina automatica (expiracao, fechamento de dia). */
    SISTEMA
}
