package br.com.alfaschool.backend.application.access.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Distribuicao de eventos ao vivo para os paineis (coordenacao, salas,
 * portaria) via Server-Sent Events.
 *
 * SSE e nao WebSocket porque o fluxo e' unidirecional (servidor -> tela) e
 * o EventSource reconecta sozinho, o que importa numa Smart TV que fica
 * ligada o dia inteiro e perde a rede de vez em quando.
 *
 * Os canais sao isolados por (tenant, topico). Uma sala jamais recebe o
 * fluxo de outra escola nem de outra sala: o topico carrega o recorte.
 *
 * Limitacao conhecida: os emitters vivem na memoria do processo. Com mais
 * de uma replica do backend sera preciso um backplane (Redis pub/sub) —
 * ate la, o deploy blue/green deve drenar as conexoes.
 */
@Service
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);

    /** 30 min: a TV reconecta sozinha e conexao eterna vaza memoria. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> canais = new ConcurrentHashMap<>();

    private String chave(UUID tenantId, String topico) {
        return tenantId + "::" + topico;
    }

    public SseEmitter inscrever(UUID tenantId, String topico) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        String chave = chave(tenantId, topico);
        CopyOnWriteArrayList<SseEmitter> lista =
                canais.computeIfAbsent(chave, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);

        Runnable remover = () -> {
            CopyOnWriteArrayList<SseEmitter> atual = canais.get(chave);
            if (atual != null) {
                atual.remove(emitter);
                if (atual.isEmpty()) {
                    canais.remove(chave);
                }
            }
        };
        emitter.onCompletion(remover);
        emitter.onTimeout(remover);
        emitter.onError(e -> remover.run());

        try {
            // Primeiro evento confirma a assinatura e derruba o buffer de
            // proxies que so' entregam depois de N bytes.
            emitter.send(SseEmitter.event().name("conectado").data(topico));
        } catch (IOException e) {
            remover.run();
        }
        return emitter;
    }

    public void publicar(UUID tenantId, String topico, String evento, Object payload) {
        CopyOnWriteArrayList<SseEmitter> lista = canais.get(chave(tenantId, topico));
        if (lista == null || lista.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : lista) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(payload));
            } catch (Exception e) {
                // Tela fechada ou rede caiu: descarta em silencio. A TV
                // reconecta e recarrega o estado inteiro pelo REST.
                lista.remove(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ja estava encerrado
                }
            }
        }
    }

    /**
     * Heartbeat: sem trafego, proxies e a propria TV derrubam a conexao
     * por inatividade sem avisar ninguem.
     */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        canais.forEach((chave, lista) -> {
            for (SseEmitter emitter : lista) {
                try {
                    emitter.send(SseEmitter.event().comment("hb"));
                } catch (Exception e) {
                    lista.remove(emitter);
                }
            }
        });
    }

    public int conexoes(UUID tenantId, String topico) {
        CopyOnWriteArrayList<SseEmitter> lista = canais.get(chave(tenantId, topico));
        return lista == null ? 0 : lista.size();
    }
}
