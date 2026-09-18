package br.com.alfaschool.backend.domain.access.permanencia;

/**
 * Como o par entrada/saida nasceu.
 *
 * EVENTO: reconstruido a partir de acc_eventos — e' apagado e refeito a
 * cada recalculo. MANUAL: correcao feita por gente, com motivo e operador
 * gravados; o motor NAO o destroi num recalculo automatico.
 *
 * Nao existe enum equivalente em domain/access/shared — a coluna
 * acc_presenca_pares.origem so' e' usada aqui.
 */
public enum OrigemPar { EVENTO, MANUAL }
