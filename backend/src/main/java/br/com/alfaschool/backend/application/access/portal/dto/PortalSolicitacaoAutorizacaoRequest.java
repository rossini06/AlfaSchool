package br.com.alfaschool.backend.application.access.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pedido da familia para incluir alguem na lista de quem pode retirar.
 *
 * <p>ISTO E' UM PEDIDO, NAO UMA LIBERACAO. Grava com origem PORTAL e status
 * PENDENTE; ninguem passa na portaria por causa deste POST. Se o portal
 * pudesse liberar sozinho, bastaria uma conta de responsavel comprometida para
 * alguem retirar uma crianca.
 */
public record PortalSolicitacaoAutorizacaoRequest(
        @NotBlank @Size(max = 160) String nome,
        @Size(max = 30) String parentesco,
        @Size(max = 20) String documento,
        @Size(max = 20) String telefone,
        LocalDate validoAte,
        /**
         * Recorte pedido pela familia. Sao PEDIDOS: quem decide continua
         * sendo a escola, na aprovacao. Antes o formulario coletava os tres
         * e a API nao os recebia — o pai marcava "segunda a sexta, das 17h
         * as 18h" e nada disso era gravado.
         */
        LocalDate validoDe,
        @Size(max = 20) String diasSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        @Size(max = 500) String justificativa
) {
}
