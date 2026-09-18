package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Todas as escritas na fila, cada uma na propria transacao.
 *
 * <p>Fica em um bean separado por causa da transacao: as operacoes rodam em
 * {@code REQUIRES_NEW} para que uma violacao de UNIQUE
 * (tenant_id, chave_idempotencia) marque apenas ESTA transacao como rollback,
 * sem contaminar a transacao de quem chamou. Enfileirar um aviso nao pode
 * derrubar o registro de uma entrada na portaria.
 *
 * <p>Convencao de retry da fatia (a V42 nao tem coluna {@code erro_permanente}):
 * {@code agendado_para = NULL} em status FALHOU significa erro permanente, e a
 * varredura nunca traz esse registro de volta.
 */
@Component
public class EnvioFilaWriter {

    private static final Logger log = LoggerFactory.getLogger(EnvioFilaWriter.class);

    private final AccNotificacaoEnvioRepository envioRepository;

    public EnvioFilaWriter(AccNotificacaoEnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    /**
     * Grava so' se a chave ainda nao existe.
     *
     * <p>A verificacao previa por {@code exists} cobre o caso comum (reprocesso
     * da fila offline do agente); o {@code catch} cobre a corrida entre dois
     * processos, que o banco resolve pelo indice unico. Nos dois casos o
     * resultado desejado e' o mesmo: exatamente uma linha, sem excecao.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AccNotificacaoEnvio> gravarSeNovo(AccNotificacaoEnvio envio) {
        if (envioRepository.existsByTenantIdAndChaveIdempotencia(envio.getTenantId(), envio.getChaveIdempotencia())) {
            log.debug("Envio ja existente para chave {} — ignorando", envio.getChaveIdempotencia());
            return Optional.empty();
        }
        try {
            return Optional.of(envioRepository.saveAndFlush(envio));
        } catch (DataIntegrityViolationException e) {
            log.debug("Corrida de idempotencia na chave {} — outro processo ja gravou",
                    envio.getChaveIdempotencia());
            return Optional.empty();
        }
    }

    /**
     * CLAIM ATOMICO. Devolve a linha somente para quem conseguiu mover
     * PENDENTE -> ENVIANDO. Dois workers competindo: um leva, o outro recebe
     * {@code Optional.empty()} e desiste sem mandar nada.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AccNotificacaoEnvio> reivindicar(UUID envioId) {
        int afetadas = envioRepository.reivindicar(envioId, StatusEnvio.PENDENTE, StatusEnvio.ENVIANDO, Instant.now());
        if (afetadas == 0) {
            return Optional.empty();
        }
        return envioRepository.findById(envioId);
    }

    /** Devolve a linha para a fila sem gastar tentativa (teto diario batido). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reagendarSemPenalidade(UUID envioId, Instant quando, String motivo) {
        envioRepository.findById(envioId).ifPresent(e -> {
            e.setStatus(StatusEnvio.PENDENTE);
            e.setAgendadoPara(quando);
            if (motivo != null) {
                e.setErro(motivo + ": reagendado sem consumir tentativa");
            }
            envioRepository.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarSucesso(UUID envioId, String providerMessageId) {
        envioRepository.findById(envioId).ifPresent(e -> {
            e.setStatus(StatusEnvio.ENVIADO);
            e.setEnviadoEm(Instant.now());
            e.setTentativas(e.getTentativas() + 1);
            e.setProviderMessageId(truncar(providerMessageId, 120));
            e.setErro(null);
            e.setAgendadoPara(null);
            envioRepository.save(e);
        });
    }

    /**
     * @param proximaTentativa null = erro permanente (ou teto de tentativas):
     *                         a linha fica FALHOU sem agendamento e a varredura
     *                         nunca a traz de volta.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(UUID envioId, String codigo, String mensagem, Instant proximaTentativa) {
        envioRepository.findById(envioId).ifPresent(e -> {
            e.setStatus(StatusEnvio.FALHOU);
            e.setTentativas(e.getTentativas() + 1);
            e.setErro(truncar((codigo == null ? "ERRO" : codigo) + ": " + (mensagem == null ? "" : mensagem), 500));
            e.setAgendadoPara(proximaTentativa);
            envioRepository.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarExpirado(UUID envioId) {
        envioRepository.findById(envioId).ifPresent(e -> {
            e.setStatus(StatusEnvio.EXPIRADO);
            e.setErro("EXPIRADO: janela util de 24h vencida");
            e.setAgendadoPara(null);
            envioRepository.save(e);
        });
    }

    /** Devolve um envio FALHOU para a fila (reenvio manual pelo operador). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reabrir(UUID envioId) {
        envioRepository.findById(envioId).ifPresent(e -> {
            e.setStatus(StatusEnvio.PENDENTE);
            e.setErro(null);
            e.setTentativas(0);
            e.setAgendadoPara(Instant.now());
            envioRepository.save(e);
        });
    }

    private String truncar(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
