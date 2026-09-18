package br.com.alfaschool.backend.domain.access.equipamento;

/**
 * Ciclo de vida de um comando na fila servidor -> agente local.
 *
 * EXPIRADA existe porque comando de portaria envelhece: abrir a catraca
 * meia hora depois do pedido e' pior do que nao abrir.
 */
public enum StatusAgentTask { PENDENTE, ENVIADA, CONCLUIDA, FALHOU, EXPIRADA }
