package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;

import java.util.UUID;

/**
 * Pedido de registro de ocorrencia publicado como evento de aplicacao.
 *
 * Existe porque as outras fatias (eventos, equipamentos, permanencia) nao
 * dependem desta: elas publicam este evento e seguem a vida. Quem quiser
 * acoplamento direto usa OcorrenciaRegistroPort; quem nao quiser, publica.
 *
 * Publique DEPOIS do commit do fato que originou a ocorrencia. Ocorrencia
 * de um evento que o banco desfez vira ruido na mesa da coordenacao.
 */
public record RegistrarOcorrenciaEvent(
        UUID tenantId,
        UUID unitId,
        TipoOcorrencia tipo,
        GravidadeOcorrencia gravidade,
        UUID alunoId,
        UUID pessoaAutorizadaId,
        UUID dispositivoId,
        UUID eventoId,
        UUID retiradaId,
        String descricao
) {
}
