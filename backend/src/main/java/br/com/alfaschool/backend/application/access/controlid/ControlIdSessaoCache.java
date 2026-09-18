package br.com.alfaschool.backend.application.access.controlid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache de sessao do Control iD, uma entrada POR DISPOSITIVO.
 *
 * Os projetos anteriores chamavam /login.fcgi antes de cada operacao. Num
 * full sync de 2000 alunos isso e' 2000 (ou 4000, contando a foto) logins
 * — cada um um round-trip e um handshake que o equipamento processa com o
 * mesmo processador que faz o reconhecimento facial. A fila da portaria
 * sente.
 *
 * TTL curto porque a sessao do firmware expira em minutos sem aviso; a
 * invalidacao explicita em 401 e' a rede de seguranca real, o TTL apenas
 * evita acumular sessao morta.
 *
 * Nao e' um cache de Spring (@Cacheable) de proposito: precisa de
 * invalidacao imperativa no meio de uma chamada HTTP que ja falhou.
 */
@Component
public class ControlIdSessaoCache {

    private record Sessao(String token, Instant expiraEm) {
        boolean valida(Instant agora) {
            return agora.isBefore(expiraEm);
        }
    }

    private final Map<UUID, Sessao> porDispositivo = new ConcurrentHashMap<>();
    private final Duration ttl;

    public ControlIdSessaoCache(@Value("${app.access.controlid.sessao-ttl-seconds:240}") long ttlSegundos) {
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    public String obter(UUID dispositivoId) {
        Sessao s = porDispositivo.get(dispositivoId);
        if (s == null) {
            return null;
        }
        if (!s.valida(Instant.now())) {
            porDispositivo.remove(dispositivoId, s);
            return null;
        }
        return s.token();
    }

    public void guardar(UUID dispositivoId, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        porDispositivo.put(dispositivoId, new Sessao(token, Instant.now().plus(ttl)));
    }

    /** Chamado em 401/403: a sessao morreu antes do TTL. */
    public void invalidar(UUID dispositivoId) {
        porDispositivo.remove(dispositivoId);
    }

    public void limparTudo() {
        porDispositivo.clear();
    }

    public int tamanho() {
        return porDispositivo.size();
    }
}
