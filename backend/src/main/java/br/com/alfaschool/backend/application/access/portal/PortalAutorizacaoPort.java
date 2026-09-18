package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.application.access.portal.dto.PortalAutorizacaoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalSolicitacaoAutorizacaoRequest;

import java.util.List;
import java.util.UUID;

/**
 * Lista de quem pode retirar o aluno e o pedido de inclusao feito pela familia.
 *
 * <p>Porta declarada pelo portal; a implementacao pertence a fatia de
 * autorizacoes. O contrato carrega a regra que nao pode ser negociada na
 * implementacao: {@link #solicitar} cria com origem PORTAL e status PENDENTE e
 * NAO libera nada.
 */
public interface PortalAutorizacaoPort {

    List<PortalAutorizacaoResumo> listar(UUID tenantId, UUID alunoId);

    /**
     * Registra um PEDIDO de inclusao.
     *
     * @return id da solicitacao criada, sempre com status PENDENTE e origem
     *         PORTAL. Nenhuma implementacao pode liberar acesso aqui: a
     *         aprovacao e' da escola.
     */
    UUID solicitar(UUID tenantId, UUID alunoId, UUID solicitanteResponsavelId,
                   PortalSolicitacaoAutorizacaoRequest request);
}
