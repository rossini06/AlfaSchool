package br.com.alfaschool.backend.application.access.permanencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reprocessamento periodico da permanencia.
 *
 * Os dois jobs existem por motivos diferentes e nao substituem um ao
 * outro: o de 10 minutos e' rede de seguranca do fluxo ao vivo (evento
 * gravado cujo listener falhou, agente que sincronizou atrasado), e o das
 * 03:00 e' o corte do dia — e' ele que transforma "aluno ainda dentro da
 * escola" em "faltou registrar a saida".
 *
 * Ambos rodam sem TenantContext: o tenant vem dos proprios dados, por
 * isso o servico recebe o tenantId explicito em vez de ler o ThreadLocal.
 */
@Component
public class PermanenciaScheduler {

    private static final Logger log = LoggerFactory.getLogger(PermanenciaScheduler.class);
    private static final String ZONE = "America/Sao_Paulo";

    private final PermanenciaService permanenciaService;

    public PermanenciaScheduler(PermanenciaService permanenciaService) {
        this.permanenciaService = permanenciaService;
    }

    /**
     * A cada 10 min, refaz os dias de quem passou pela portaria hoje.
     * Incremental por recorte, nao por estado: so' entram os alunos com
     * evento no dia corrente.
     */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT2M")
    public void recalcularDiaCorrente() {
        LocalDate hoje = LocalDate.now(java.time.ZoneId.of(ZONE));
        int total = 0;
        for (UUID tenantId : permanenciaService.tenantsComMovimento(hoje)) {
            try {
                total += permanenciaService.recalcularDiaCorrente(tenantId, hoje);
            } catch (RuntimeException e) {
                // Um tenant com dado ruim nao pode parar a varredura dos outros.
                log.error("Falha ao recalcular permanencia do tenant {} em {}", tenantId, hoje, e);
            }
        }
        if (total > 0) {
            log.debug("Recalculo incremental de {}: {} dias atualizados", hoje, total);
        }
    }

    /**
     * 03:00 America/Sao_Paulo: fecha o dia anterior. O que sobrou ABERTA
     * vira INCONSISTENTE — ninguem dorme na escola, e um par impar num dia
     * que passou e' saida nao registrada.
     *
     * As 03:00 e nao a' meia-noite de proposito: evento de agente offline
     * costuma chegar na virada, e fechar cedo demais criaria pendencia
     * que se resolveria sozinha meia hora depois.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = ZONE)
    public void fecharDiaAnterior() {
        LocalDate ontem = LocalDate.now(java.time.ZoneId.of(ZONE)).minusDays(1);
        try {
            int total = permanenciaService.fecharDiasAnteriores(ontem);
            log.info("Fechamento diario de {}: {} dias reprocessados", ontem, total);
        } catch (RuntimeException e) {
            log.error("Falha no fechamento diario de {}", ontem, e);
        }
    }
}
