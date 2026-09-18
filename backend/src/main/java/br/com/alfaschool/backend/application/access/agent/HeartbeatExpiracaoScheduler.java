package br.com.alfaschool.backend.application.access.agent;

import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Expira o campo {@code online} dos equipamentos.
 *
 * BUG REAL QUE ESTA CLASSE CORRIGE: nos sistemas anteriores do grupo,
 * online era marcado true no heartbeat e NUNCA voltava a false sozinho.
 * Agente morto, micro da portaria desligado, cabo de rede solto — o
 * painel continuava mostrando o leitor verde, para sempre. A coordenacao
 * so' descobria que a catraca estava muda quando alguem reclamava
 * pessoalmente.
 *
 * A regra e' simples e deliberadamente burra: sem heartbeat ha mais de
 * {@code app.access.heartbeat-timeout-seconds}, o equipamento e' offline.
 * Nao ha tentativa de sondar o leitor a partir da nuvem — ele esta numa
 * rede 192.168.x que o backend nao alcanca, e a sondagem so' gastaria o
 * timeout que ja custou caro nos projetos anteriores.
 *
 * Equipamento que nunca mandou heartbeat (ultimo_heartbeat NULL) tambem
 * cai, porque "nunca deu sinal" nao e' melhor que "parou de dar sinal".
 */
@Component
public class HeartbeatExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatExpiracaoScheduler.class);

    private final DispositivoRepository dispositivos;
    private final Duration timeout;

    public HeartbeatExpiracaoScheduler(
            DispositivoRepository dispositivos,
            @Value("${app.access.heartbeat-timeout-seconds:180}") long timeoutSegundos) {
        this.dispositivos = dispositivos;
        this.timeout = Duration.ofSeconds(timeoutSegundos);
    }

    /**
     * Roda com intervalo bem menor que o timeout: se rodasse a cada 180 s
     * com timeout de 180 s, um leitor poderia ficar ate' 6 minutos
     * mostrando verde depois de morto.
     */
    @Scheduled(fixedDelayString = "${app.access.heartbeat-check-ms:30000}")
    @Transactional
    public void expirarOffline() {
        Instant limite = Instant.now().minus(timeout);
        // Varredura global de proposito: o scheduler roda fora de
        // requisicao, sem TenantContext, e a expiracao vale para todas as
        // escolas. O TenantRepositoryAspect nao aplica filtro quando o
        // contexto esta vazio, entao findAll() enxerga tudo.
        List<Dispositivo> ativos = dispositivos.findAll();

        int derrubados = 0;
        for (Dispositivo d : ativos) {
            if (Boolean.TRUE.equals(d.getDeleted()) || !d.isOnline()) {
                continue;
            }
            Instant ultimo = d.getUltimoHeartbeat();
            if (ultimo != null && ultimo.isAfter(limite)) {
                continue;
            }
            d.setOnline(false);
            dispositivos.save(d);
            derrubados++;
            log.warn("Equipamento {} ({}) marcado OFFLINE: sem heartbeat desde {}.",
                    d.getNome(), d.getId(), ultimo == null ? "nunca" : ultimo);
        }
        if (derrubados > 0) {
            log.info("{} equipamento(s) expiraram por falta de heartbeat.", derrubados);
        }
    }
}
