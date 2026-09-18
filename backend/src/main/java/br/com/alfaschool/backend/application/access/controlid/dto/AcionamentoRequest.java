package br.com.alfaschool.backend.application.access.controlid.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Pedido de acionamento vindo da API.
 *
 * Nao vira comando direto: e' convertido em AcionamentoAcesso, que rejeita
 * tipo fora da whitelist e parametro fora da regex. O campo "parametros"
 * do firmware e' texto livre; aceitar o que chega aqui seria expor o
 * console do equipamento.
 */
public record AcionamentoRequest(
        @NotBlank(message = "Tipo de acionamento é obrigatório.") String tipo,
        @NotBlank(message = "Parâmetros são obrigatórios.") String parametros) {
}
