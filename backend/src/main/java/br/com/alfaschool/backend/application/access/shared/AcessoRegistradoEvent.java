package br.com.alfaschool.backend.application.access.shared;

import br.com.alfaschool.backend.domain.access.shared.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado depois que um evento de leitura foi gravado e deduplicado.
 *
 * E' o ponto de desacoplamento do modulo: quem ingere o evento (Control iD,
 * agente, webhook, simulador) nao conhece permanencia, fila de retirada nem
 * notificacao. Cada um desses reage a este evento.
 *
 * Publicar SEMPRE apos o commit da transacao que gravou o evento
 * (@TransactionalEventListener(phase = AFTER_COMMIT)), senao um rollback
 * deixa painel e notificacao falando de um evento que nao existe.
 */
public record AcessoRegistradoEvent(
        UUID tenantId,
        UUID unitId,
        UUID eventoId,
        UUID dispositivoId,
        UUID portariaId,
        FuncaoDispositivo funcaoDispositivo,
        TitularTipo titularTipo,
        UUID titularId,
        ResultadoAcesso resultado,
        SentidoAcesso sentido,
        String motivo,
        Instant dataHora
) {
    public boolean permitido() {
        return resultado == ResultadoAcesso.PERMITIDO;
    }

    /** Leitura num leitor exclusivo de responsavel abre a fila de retirada. */
    public boolean chegadaDeResponsavel() {
        return funcaoDispositivo == FuncaoDispositivo.RESPONSAVEL
                && (titularTipo == TitularTipo.RESPONSAVEL || titularTipo == TitularTipo.AUTORIZADA);
    }
}
