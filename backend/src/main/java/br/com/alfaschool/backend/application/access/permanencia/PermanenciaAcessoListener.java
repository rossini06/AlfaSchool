package br.com.alfaschool.backend.application.access.permanencia;

import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.application.access.shared.PermanenciaPort;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Liga a leitura na catraca a' apuracao de permanencia.
 *
 * Sem esta classe o sistema grava o evento, abre a fila de retirada e
 * avisa a familia, mas o aluno nunca aparece como presente e nenhuma hora
 * e' contada — foi exatamente o que o teste de ponta a ponta pegou.
 *
 * So' reage a aluno com acesso PERMITIDO. Leitura de responsavel abre
 * retirada, nao presenca; acesso negado vira ocorrencia, nao hora.
 *
 * Roda depois do commit do evento: se a transacao que gravou a leitura
 * for desfeita, nao pode sobrar presenca falando de um evento inexistente.
 */
@Component
public class PermanenciaAcessoListener {

    private static final Logger log = LoggerFactory.getLogger(PermanenciaAcessoListener.class);
    private static final ZoneId FUSO_ESCOLA = ZoneId.of("America/Sao_Paulo");

    private final PermanenciaPort permanencia;

    public PermanenciaAcessoListener(PermanenciaPort permanencia) {
        this.permanencia = permanencia;
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
        if (evento.titularTipo() != TitularTipo.ALUNO || evento.titularId() == null || !evento.permitido()) {
            return;
        }
        UUID tenantAnterior = TenantContext.getTenantId();
        try {
            // O listener roda fora do request que originou o evento (o agente
            // ou o webhook ja devolveu). O tenant vem do proprio evento.
            TenantContext.setTenantId(evento.tenantId());
            switch (evento.sentido()) {
                case ENTRADA -> permanencia.registrarEntrada(
                        evento.tenantId(), evento.titularId(), evento.dataHora(), evento.eventoId());
                case SAIDA -> permanencia.registrarSaida(
                        evento.tenantId(), evento.titularId(), evento.dataHora(), evento.eventoId());
                // Leitor sem sentido configurado: o motor pareia por ordem
                // de relogio, entao reconstruir o dia resolve sozinho.
                case INDEFINIDO -> permanencia.recalcularDia(
                        evento.tenantId(), evento.titularId(), diaDoEvento(evento));
            }
        } catch (RuntimeException e) {
            // Falhar aqui nao pode desfazer a leitura ja gravada: o evento e'
            // o livro-razao e a permanencia e' derivada dele. O job de
            // recalculo reconstroi o dia depois.
            log.error("Falha ao apurar permanencia do aluno {} a partir do evento {}: {}",
                    evento.titularId(), evento.eventoId(), e.getMessage(), e);
        } finally {
            if (tenantAnterior == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(tenantAnterior);
            }
        }
    }

    private LocalDate diaDoEvento(AcessoRegistradoEvent evento) {
        return evento.dataHora().atZone(FUSO_ESCOLA).toLocalDate();
    }
}
