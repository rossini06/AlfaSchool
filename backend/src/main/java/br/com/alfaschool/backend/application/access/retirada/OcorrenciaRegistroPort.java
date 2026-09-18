package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.domain.access.retirada.AccOcorrencia;

/**
 * Entrada simples para qualquer fatia registrar uma ocorrencia.
 *
 * Exposta como bean com interface para que quem depende dela nao precise
 * conhecer JPA, controller nem DTO — so' o fato que aconteceu.
 *
 * Contrato: registrar NUNCA lanca para o chamador. Uma falha ao gravar a
 * ocorrencia nao pode derrubar o registro de uma entrada na portaria nem a
 * entrega de uma crianca.
 */
public interface OcorrenciaRegistroPort {

    AccOcorrencia registrar(RegistrarOcorrenciaEvent pedido);
}
