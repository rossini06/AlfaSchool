package br.com.alfaschool.backend.application.access.painel;

import br.com.alfaschool.backend.application.access.retirada.RetiradaConsultaService;
import br.com.alfaschool.backend.application.access.retirada.RetiradaStatusMudouEvent;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.application.access.shared.SseHub;
import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.shared.TipoPainel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelFonteRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Leva a mudanca de status para as TVs.
 *
 * AFTER_COMMIT e' obrigatorio: publicar antes do commit faria a tela da
 * sala anunciar "Joao pronto" para uma transacao que o banco ainda pode
 * desfazer — e a professora ja' teria descido com a crianca.
 *
 * Roteamento: vai para os paineis cujas fontes cobrem aquele aluno (sala
 * atual, turma, portaria, unidade) e para os paineis de COORDENACAO da
 * unidade, que acompanham tudo.
 */
@Component
public class PainelPublicador {

    private static final Logger log = LoggerFactory.getLogger(PainelPublicador.class);

    private final AccPainelFonteRepository fonteRepository;
    private final AccPainelRepository painelRepository;
    private final RetiradaConsultaService consultaService;
    private final SseHub sseHub;

    public PainelPublicador(AccPainelFonteRepository fonteRepository,
                            AccPainelRepository painelRepository,
                            RetiradaConsultaService consultaService,
                            SseHub sseHub) {
        this.fonteRepository = fonteRepository;
        this.painelRepository = painelRepository;
        this.consultaService = consultaService;
        this.sseHub = sseHub;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoMudarStatus(RetiradaStatusMudouEvent evento) {
        try {
            publicar(evento);
        } catch (Exception e) {
            // A TV se recupera sozinha: reconecta e recarrega o estado pelo
            // endpoint /estado. Falha aqui nao pode escalar.
            log.error("Falha ao publicar retirada {} nos paineis", evento.retiradaId(), e);
        }
    }

    /** Separado do listener para poder ser exercitado em teste sem transacao. */
    public void publicar(RetiradaStatusMudouEvent evento) {
        Set<UUID> destinos = new LinkedHashSet<>(fonteRepository.paineisQueCobrem(
                evento.tenantId(), evento.unitId(), evento.turmaId(), evento.salaId(), evento.portariaId()));

        // Coordenacao ve a unidade inteira mesmo sem fonte cadastrada: e' a
        // mesa que precisa enxergar a fila toda.
        if (evento.unitId() != null) {
            painelRepository
                    .findByTenantIdAndUnitIdAndTipoAndAtivoTrueAndDeletedFalse(
                            evento.tenantId(), evento.unitId(), TipoPainel.COORDENACAO)
                    .stream().map(AccPainel::getId).forEach(destinos::add);
        }

        if (destinos.isEmpty()) {
            return;
        }

        // Payload enxuto e sem bytes de imagem: so' ids, nomes, horarios e
        // CHAVES de foto. A TV busca a imagem por outro caminho.
        RetiradaFilaItem cartao = consultaService.porId(evento.tenantId(), evento.retiradaId());
        Object payload = cartao != null ? cartao : new CartaoMinimo(
                evento.retiradaId(), evento.alunoId(), evento.statusNovo().name(), Instant.now());

        String nomeEvento = evento.nomeSse();
        for (UUID painelId : destinos) {
            sseHub.publicar(evento.tenantId(), "painel:" + painelId, nomeEvento, payload);
        }
    }

    /**
     * Fallback para quando o cartao nao pode ser relido (retirada removida
     * entre o commit e a publicacao). A TV ainda consegue tirar o cartao da
     * tela com o id e o status.
     */
    record CartaoMinimo(UUID id, UUID alunoId, String status, Instant em) {
    }
}
