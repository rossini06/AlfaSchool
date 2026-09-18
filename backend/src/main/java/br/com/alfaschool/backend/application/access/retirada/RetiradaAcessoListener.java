package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ponte entre a leitura na portaria e a fila de retirada.
 *
 * AFTER_COMMIT: se a gravacao do evento de leitura der rollback, nao pode
 * sobrar uma retirada aberta para um evento que nao existe.
 */
@Component
public class RetiradaAcessoListener {

    private static final Logger log = LoggerFactory.getLogger(RetiradaAcessoListener.class);

    private final RetiradaService retiradaService;

    public RetiradaAcessoListener(RetiradaService retiradaService) {
        this.retiradaService = retiradaService;
    }

    /**
     * REQUIRES_NEW e' obrigatorio aqui, nao e' preferencia.
     *
     * Um listener de AFTER_COMMIT roda quando a transacao que gravou o
     * evento JA foi encerrada. Um @Transactional comum chamado deste ponto
     * tenta aderir aquela transacao morta: o codigo executa, os logs dizem
     * que deu certo e NADA e' persistido, em silencio. Foi assim que a
     * permanencia e a fila de retirada calculavam tudo e nao gravavam
     * linha nenhuma.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoRegistrarAcesso(AcessoRegistradoEvent evento) {
        try {
            if (evento.chegadaDeResponsavel()) {
                retiradaService.abrirPorReconhecimento(evento);
                return;
            }
            // Saida do aluno pela catraca: fecha o carimbo saida_em da
            // retirada. A permanencia e' encerrada pela fatia dela, que
            // escuta o mesmo evento — aqui nao se chama registrarSaida.
            if (evento.titularTipo() == TitularTipo.ALUNO
                    && evento.sentido() == SentidoAcesso.SAIDA
                    && evento.funcaoDispositivo() != FuncaoDispositivo.RESPONSAVEL
                    && evento.permitido()) {
                retiradaService.marcarSaidaPorEvento(evento);
            }
        } catch (Exception e) {
            // Listener de AFTER_COMMIT nao tem para quem devolver erro. Cair
            // aqui nao pode desfazer o evento ja' gravado.
            log.error("Falha ao processar AcessoRegistradoEvent {} na fila de retirada", evento.eventoId(), e);
        }
    }
}
