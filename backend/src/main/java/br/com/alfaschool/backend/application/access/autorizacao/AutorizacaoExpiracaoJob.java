package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.domain.access.autorizacao.AcaoAutorizacao;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Marca como EXPIRADA toda autorizacao ATIVA cuja vigenciaFim ja passou.
 *
 * O job e' higiene, nao a barreira: AutorizacaoConsultaService ja compara a
 * vigencia a cada verificacao, entao uma autorizacao vencida nunca libera
 * ninguem mesmo que o job falhe por dias. O valor daqui e' deixar o status
 * coerente nas telas e no historico.
 *
 * Roda FORA de requisicao: nao ha TenantContext nem usuario, por isso varre
 * todos os tenants e grava historico com userId nulo.
 */
@Component
public class AutorizacaoExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoExpiracaoJob.class);

    private static final String MOTIVO = "Expiracao automatica";

    private final AccAutorizacaoRetiradaRepository autorizacaoRepository;
    private final AccAutorizacaoHistoricoRepository historicoRepository;

    public AutorizacaoExpiracaoJob(AccAutorizacaoRetiradaRepository autorizacaoRepository,
                                   AccAutorizacaoHistoricoRepository historicoRepository) {
        this.autorizacaoRepository = autorizacaoRepository;
        this.historicoRepository = historicoRepository;
    }

    /**
     * 00:10 no fuso da escola. A data de corte tambem e' a de Sao Paulo: usar
     * a data do servidor em UTC expiraria as autorizacoes do dia corrente
     * durante as primeiras horas da noite.
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void expirarVencidas() {
        LocalDate hoje = LocalDate.now(AutorizacaoConsultaService.FUSO_ESCOLA);

        List<AutorizacaoRetirada> vencidas = autorizacaoRepository
                .findByStatusAndVigenciaFimBeforeAndDeletedFalse(StatusAutorizacao.ATIVA, hoje);
        if (vencidas.isEmpty()) {
            return;
        }

        for (AutorizacaoRetirada autorizacao : vencidas) {
            StatusAutorizacao anterior = autorizacao.getStatus();
            autorizacao.setStatus(StatusAutorizacao.EXPIRADA);
            autorizacaoRepository.save(autorizacao);
            historicoRepository.save(new AutorizacaoHistorico(
                    autorizacao.getTenantId(),
                    autorizacao.getId(),
                    AcaoAutorizacao.EXPIRACAO,
                    anterior,
                    StatusAutorizacao.EXPIRADA,
                    null,
                    MOTIVO,
                    null));
        }
        log.info("Expiracao automatica de autorizacoes de retirada: {} marcadas como EXPIRADA", vencidas.size());
    }
}
