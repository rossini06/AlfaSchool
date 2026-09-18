package br.com.alfaschool.backend.domain.access.equipamento;

/**
 * Comandos que o servidor pode pedir ao agente local. Lista fechada de
 * proposito: o agente executa o que chega da fila, entao aceitar tipo
 * arbitrario seria execucao remota disfarcada de tarefa.
 */
public enum TipoAgentTask {
    SINCRONIZAR_PESSOA,
    REMOVER_PESSOA,
    SINCRONIZAR_FACE,
    REMOVER_FACE,
    ACIONAR_ACESSO,
    COLETAR_LOGS,
    REINICIAR_DISPOSITIVO
}
