package br.com.alfaschool.backend.application.access.retirada.dto;

/**
 * Confirmacao de entrega. Nao carrega quem entregou: isso vem do usuario
 * autenticado, nunca do corpo da requisicao — senao bastaria mandar o id de
 * outro colaborador para transferir a responsabilidade.
 */
public record EntregaRequest(
        String observacao
) {
}
