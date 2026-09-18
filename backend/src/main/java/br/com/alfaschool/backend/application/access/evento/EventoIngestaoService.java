package br.com.alfaschool.backend.application.access.evento;

import br.com.alfaschool.backend.application.access.evento.dto.LeituraBruta;
import br.com.alfaschool.backend.application.access.evento.dto.ResultadoIngestao;
import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.evento.AccEvento;
import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccEventoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caminho unico de entrada de eventos de leitura.
 *
 * Webhook do equipamento, agente local, polling direto e simulador
 * chegam todos aqui. Isso e' o que faz o simulador valer alguma coisa:
 * ele exercita o mesmo codigo que o hardware exercita.
 *
 * Tres decisoes carregam cicatriz de producao e estao documentadas nos
 * metodos: deduplicacao com janela, conversao do epoch local do firmware
 * e resolucao honesta do titular.
 */
@Service
public class EventoIngestaoService {

    private static final Logger log = LoggerFactory.getLogger(EventoIngestaoService.class);

    private final AccEventoRepository eventos;
    private final AccFaceRepository faces;
    private final DispositivoRepository dispositivos;
    private final ApplicationEventPublisher publisher;
    private final ZoneId zonaDoEquipamento;
    private final Duration janelaDedup;

    public EventoIngestaoService(AccEventoRepository eventos,
                                 AccFaceRepository faces,
                                 DispositivoRepository dispositivos,
                                 ApplicationEventPublisher publisher,
                                 @Value("${app.access.device-timezone:America/Sao_Paulo}") String deviceTimezone,
                                 @Value("${app.access.dedup-window-seconds:60}") long dedupWindowSeconds) {
        this.eventos = eventos;
        this.faces = faces;
        this.dispositivos = dispositivos;
        this.publisher = publisher;
        this.zonaDoEquipamento = ZoneId.of(deviceTimezone);
        this.janelaDedup = Duration.ofSeconds(dedupWindowSeconds);
    }

    /**
     * Grava uma leitura.
     *
     * @return o evento gravado, ou o evento ja existente quando a leitura
     *         e' reconhecida como replay.
     */
    @Transactional
    public ResultadoIngestao registrar(UUID tenantId, LeituraBruta leitura) {
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado.");
        }
        Dispositivo dispositivo = dispositivos.findById(leitura.dispositivoId())
                .filter(d -> tenantId.equals(d.getTenantId()))
                .filter(d -> Boolean.FALSE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipamento não encontrado para este tenant."));

        Instant agora = Instant.now();
        Instant dataHora = resolverDataHora(leitura, agora, dispositivo);

        // Deduplicacao ANTES de qualquer efeito colateral: o evento
        // publicado dispara presenca, fila de retirada e notificacao a
        // familia. Avisar duas vezes que a crianca entrou e' pior do que
        // nao avisar.
        Optional<AccEvento> replay = procurarReplay(dispositivo.getId(), leitura.deviceLogId(), dataHora);
        if (replay.isPresent()) {
            log.debug("Leitura {} do equipamento {} é replay do evento {}",
                    leitura.deviceLogId(), dispositivo.getNome(), replay.get().getId());
            return ResultadoIngestao.replay(replay.get());
        }

        AccEvento evento = new AccEvento();
        evento.setTenantId(tenantId);
        evento.setUnitId(dispositivo.getUnitId());
        evento.setDispositivoId(dispositivo.getId());
        evento.setPortariaId(dispositivo.getPortariaId());
        evento.setDeviceLogId(leitura.deviceLogId());
        evento.setDeviceUserId(leitura.deviceUserId());
        evento.setDataHora(dataHora);
        evento.setRecebidoEm(agora);
        evento.setTipo(leitura.tipo() == null ? TipoIdentificacao.FACE : leitura.tipo());
        evento.setOrigem(leitura.origem() == null ? OrigemEvento.AGENTE : leitura.origem());
        evento.setMotivo(leitura.motivo());
        evento.setRawJson(leitura.rawJson());
        evento.setSentido(resolverSentido(leitura, dispositivo));
        evento.setResultado(resolverResultado(leitura));

        aplicarTitular(evento, tenantId, leitura.deviceUserId());

        AccEvento gravado = eventos.save(evento);

        // Publicado DENTRO da transacao, mas so' entregue depois do
        // commit: os consumidores usam
        // @TransactionalEventListener(AFTER_COMMIT). Publicar de forma
        // sincrona antes do commit faria painel e notificacao falarem de
        // um evento que o rollback ainda pode apagar.
        publisher.publishEvent(new AcessoRegistradoEvent(
                gravado.getTenantId(),
                gravado.getUnitId(),
                gravado.getId(),
                gravado.getDispositivoId(),
                gravado.getPortariaId(),
                dispositivo.getFuncao(),
                gravado.getTitularTipo(),
                gravado.getTitularId(),
                gravado.getResultado(),
                gravado.getSentido(),
                gravado.getMotivo(),
                gravado.getDataHora()));

        return ResultadoIngestao.gravado(gravado);
    }

    // =================================================================
    // Titular
    // =================================================================

    /**
     * Resolve quem passou a partir do device_user_id, via acc_faces.
     *
     * Quando nao ha face correspondente o evento fica DESCONHECIDO com
     * titular_id NULL. Um dos sistemas anteriores gravava o proprio
     * device_user_id na coluna aluno_id: como o numero "existia", os
     * relatorios juntavam esse evento com o aluno cujo UUID nunca foi
     * aquele, e o banco ganhou FK apontando para o nada. Inventar id e'
     * pior do que admitir que nao se sabe.
     */
    private void aplicarTitular(AccEvento evento, UUID tenantId, Long deviceUserId) {
        if (deviceUserId == null || deviceUserId <= 0) {
            evento.setTitularTipo(TitularTipo.DESCONHECIDO);
            evento.setTitularId(null);
            return;
        }
        Optional<AccFace> face = faces.findByTenantIdAndDeviceUserIdAndDeletedFalse(tenantId, deviceUserId);
        if (face.isEmpty()) {
            evento.setTitularTipo(TitularTipo.DESCONHECIDO);
            evento.setTitularId(null);
            if (evento.getMotivo() == null) {
                evento.setMotivo("Usuário " + deviceUserId + " não cadastrado neste tenant.");
            }
            return;
        }
        evento.setTitularTipo(face.get().getTitularTipo());
        evento.setTitularId(face.get().getTitularId());
    }

    // =================================================================
    // Deduplicacao
    // =================================================================

    /**
     * E' replay quando existe evento do MESMO dispositivo com o MESMO
     * device_log_id dentro da janela de +/- {@code dedup-window-seconds}.
     *
     * Por que a janela, e por que nao um UNIQUE no banco:
     *
     * O device_log_id e' o id da linha dentro do equipamento. Ele NAO e'
     * estavel para sempre — limpar o historico do leitor reinicia o
     * contador em 1. Com um UNIQUE em (dispositivo_id, device_log_id),
     * todo evento posterior a uma limpeza colidiria com um evento antigo
     * e seria descartado em silencio: um buraco negro permanente no
     * livro-razao, exatamente na escola que acabou de fazer manutencao no
     * equipamento.
     *
     * A janela distingue os dois casos: reenvio do mesmo lote chega com o
     * mesmo horario (segundos de diferenca), enquanto um contador
     * reiniciado produz o mesmo id com horario de HOJE, longe do evento
     * antigo. Por isso um device_log_id MENOR que o ultimo visto tambem e'
     * aceito normalmente — a monotonicidade do contador nao e' garantida
     * e nao entra na decisao.
     */
    private Optional<AccEvento> procurarReplay(UUID dispositivoId, Long deviceLogId, Instant dataHora) {
        if (deviceLogId == null) {
            // Sem id do equipamento nao ha como afirmar que e' o mesmo
            // registro. Gravar duplicado e' recuperavel; descartar
            // evento real nao e'.
            return Optional.empty();
        }
        List<AccEvento> candidatos = eventos.buscarReplay(
                dispositivoId, deviceLogId,
                dataHora.minus(janelaDedup), dataHora.plus(janelaDedup));
        return candidatos.isEmpty() ? Optional.empty() : Optional.of(candidatos.get(0));
    }

    // =================================================================
    // Tempo
    // =================================================================

    /**
     * Prioriza o epoch do equipamento (convertido do relogio local) e cai
     * para o instante explicito da origem. Depois grampeia futuro.
     */
    private Instant resolverDataHora(LeituraBruta leitura, Instant agora, Dispositivo dispositivo) {
        Instant lido;
        if (leitura.epochLocalEquipamento() != null) {
            lido = RelogioEquipamento.doEpochLocal(leitura.epochLocalEquipamento(), zonaDoEquipamento);
        } else if (leitura.dataHora() != null) {
            lido = leitura.dataHora();
        } else {
            lido = agora;
        }
        if (RelogioEquipamento.noFuturo(lido, agora)) {
            log.warn("Equipamento {} reportou leitura no futuro ({}); usando a hora do servidor. "
                            + "Verifique o relógio/NTP do leitor.",
                    dispositivo.getNome(), lido);
            return agora;
        }
        // Passado e' legitimo: fila offline do agente reenviando o dia.
        return lido;
    }

    // =================================================================
    // Interpretacao
    // =================================================================

    private SentidoAcesso resolverSentido(LeituraBruta leitura, Dispositivo dispositivo) {
        if (leitura.sentido() != null) {
            return leitura.sentido();
        }
        // O sentido e' propriedade do POSICIONAMENTO do leitor, nao do
        // evento: o firmware nao sabe se a catraca esta na entrada.
        return dispositivo.getSentido() == null ? SentidoAcesso.INDEFINIDO : dispositivo.getSentido();
    }

    private ResultadoAcesso resolverResultado(LeituraBruta leitura) {
        if (leitura.resultado() != null) {
            return leitura.resultado();
        }
        return resultadoDoEventCode(leitura.eventCode());
    }

    /**
     * Traducao do campo "event" do access_logs.
     *
     * As duas implementacoes anteriores do grupo discordavam nos codigos
     * 11 e 12 (uma dizia "concedido", a outra "lista de bloqueio"). Vale
     * a tabela do driver do agente, que e' a que roda ha mais tempo em
     * campo com iDFace:
     *   1..4, 7  -> acesso concedido (cartao, senha, digital, face)
     *   5, 6     -> negado por regra; o metodo nao e' confiavel
     *   8        -> face nao reconhecida
     *   9..12    -> lista de bloqueio
     * Sem codigo, o padrao e' DESCONHECIDO: presumir PERMITIDO encheria a
     * presenca de entradas que nunca aconteceram.
     */
    public static ResultadoAcesso resultadoDoEventCode(Integer code) {
        if (code == null) {
            return ResultadoAcesso.DESCONHECIDO;
        }
        return switch (code) {
            case 1, 2, 3, 4, 7 -> ResultadoAcesso.PERMITIDO;
            case 5, 6, 8, 9, 10, 11, 12 -> ResultadoAcesso.NEGADO;
            default -> ResultadoAcesso.DESCONHECIDO;
        };
    }

    /** Exposto para o polling montar filtros no fuso do equipamento. */
    public ZoneId zonaDoEquipamento() {
        return zonaDoEquipamento;
    }

    public Duration janelaDedup() {
        return janelaDedup;
    }
}
