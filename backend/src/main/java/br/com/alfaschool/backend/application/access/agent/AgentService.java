package br.com.alfaschool.backend.application.access.agent;

import br.com.alfaschool.backend.application.access.agent.dto.AgentDtos;
import br.com.alfaschool.backend.application.access.biometria.FaceService;
import br.com.alfaschool.backend.application.access.biometria.FotoStorage;
import br.com.alfaschool.backend.application.access.evento.EventoIngestaoService;
import br.com.alfaschool.backend.application.access.evento.dto.LeituraBruta;
import br.com.alfaschool.backend.application.access.evento.dto.ResultadoIngestao;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.equipamento.AccAgentTask;
import br.com.alfaschool.backend.domain.access.equipamento.StatusAgentTask;
import br.com.alfaschool.backend.domain.access.shared.OrigemEvento;
import br.com.alfaschool.backend.domain.access.shared.TipoIdentificacao;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAgentTaskRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Lado servidor do protocolo do agente local.
 *
 * O backend vive na nuvem e nao alcanca a LAN da escola. Toda a conversa
 * e' iniciada pelo agente: ele busca o que sincronizar, empurra o que
 * leu e pega comandos numa fila. Nao ha callback do servidor para dentro
 * da rede da escola, e o desenho assume isso em vez de tentar contorna-lo.
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    /** Lote do delta. Grande demais e o agente estoura memoria num Raspberry. */
    private static final int TAMANHO_PAGINA = 100;

    private final DispositivoRepository dispositivos;
    private final AccFaceRepository faces;
    private final AccAgentTaskRepository tarefas;
    private final FotoStorage fotos;
    private final EventoIngestaoService ingestao;
    private final SegredoCifrador cifrador;

    public AgentService(DispositivoRepository dispositivos,
                        AccFaceRepository faces,
                        AccAgentTaskRepository tarefas,
                        FotoStorage fotos,
                        EventoIngestaoService ingestao,
                        SegredoCifrador cifrador) {
        this.dispositivos = dispositivos;
        this.faces = faces;
        this.tarefas = tarefas;
        this.fotos = fotos;
        this.ingestao = ingestao;
        this.cifrador = cifrador;
    }

    // =================================================================
    // Equipamentos
    // =================================================================

    /**
     * Equipamentos da unidade do agente.
     *
     * @param incluirSenha quando true, a senha de administracao do
     *        equipamento vai em CLARO no payload. Ver o javadoc de
     *        {@link AgentDtos.DeviceDto} — exige HTTPS e cada uso e'
     *        logado para que o vazamento nao seja silencioso.
     */
    @Transactional(readOnly = true)
    public List<AgentDtos.DeviceDto> devices(UUID tenantId, UUID unitId, boolean incluirSenha) {
        if (incluirSenha) {
            log.warn("Agente da unidade {} solicitou as senhas dos equipamentos em claro. "
                    + "Confirme que a rota está sob HTTPS e sem body logging no proxy.", unitId);
        }
        List<Dispositivo> todos = dispositivos
                .findByTenantIdAndDeletedFalse(tenantId, Pageable.unpaged())
                .getContent();
        List<AgentDtos.DeviceDto> saida = new ArrayList<>();
        for (Dispositivo d : todos) {
            if (!d.isAtivo()) {
                continue;
            }
            // unitId null na credencial = agente da escola inteira.
            if (unitId != null && !unitId.equals(d.getUnitId())) {
                continue;
            }
            String senha = null;
            if (incluirSenha && d.getSenhaCifrada() != null) {
                senha = cifrador.decifrar(d.getSenhaCifrada());
                if (senha == null) {
                    log.warn("Senha do equipamento {} não pôde ser decifrada (chave rotacionada?).",
                            d.getNome());
                }
            }
            saida.add(AgentDtos.DeviceDto.from(d, senha));
        }
        return saida;
    }

    // =================================================================
    // Delta de pessoas
    // =================================================================

    /**
     * Pessoas e faces alteradas depois de {@code updatedAfter}.
     *
     * Faces sem base legal ou sem consentimento NAO entram na lista, em
     * silencio para o agente e com log aqui. O agente e' um executor: ele
     * nao pode ter a opcao de exportar um dado que a lei nao permite
     * exportar.
     */
    @Transactional(readOnly = true)
    public AgentDtos.UsersPage users(UUID tenantId, Instant updatedAfter) {
        Instant referencia = updatedAfter == null ? Instant.EPOCH : updatedAfter;
        // Busca uma a mais para saber se ha proxima pagina sem um count.
        List<AccFace> lote = faces
                .findByTenantIdAndUpdatedAtGreaterThanAndDeletedFalseOrderByUpdatedAtAsc(
                        tenantId, referencia, PageRequest.of(0, TAMANHO_PAGINA + 1));

        boolean temMais = lote.size() > TAMANHO_PAGINA;
        if (temMais) {
            lote = lote.subList(0, TAMANHO_PAGINA);
        }

        List<AgentDtos.UserDto> itens = new ArrayList<>();
        int bloqueadas = 0;
        for (AccFace f : lote) {
            String motivo = FaceService.motivoDeBloqueio(f);
            if (motivo != null) {
                bloqueadas++;
                continue;
            }
            byte[] conteudo;
            try {
                conteudo = fotos.ler(f.getFotoKey());
            } catch (RuntimeException e) {
                log.warn("Foto da face {} indisponível no armazenamento: {}", f.getId(), e.getMessage());
                continue;
            }
            itens.add(new AgentDtos.UserDto(
                    f.getId(),
                    f.getDeviceUserId(),
                    f.getTitularTipo(),
                    f.getTitularId(),
                    f.getTitularTipo().name() + "-" + f.getDeviceUserId(),
                    Base64.getEncoder().encodeToString(conteudo),
                    FaceService.hashDaFoto(conteudo),
                    f.isAtivo(),
                    f.getUpdatedAt()));
        }
        if (bloqueadas > 0) {
            log.info("{} face(s) omitidas do delta do agente por falta de base legal/consentimento.",
                    bloqueadas);
        }

        Instant cursor = lote.isEmpty() ? referencia : lote.get(lote.size() - 1).getUpdatedAt();
        return new AgentDtos.UsersPage(itens, cursor, temMais);
    }

    // =================================================================
    // Eventos
    // =================================================================

    public AgentDtos.EventResponse evento(UUID tenantId, AgentDtos.EventRequest req) {
        LeituraBruta leitura = new LeituraBruta(
                req.dispositivoId(),
                req.deviceLogId(),
                req.deviceUserId(),
                req.time(),
                null,
                req.event(),
                null,
                TipoIdentificacao.FACE,
                null,
                req.motivo(),
                OrigemEvento.AGENTE,
                req.raw());
        ResultadoIngestao r = ingestao.registrar(tenantId, leitura);
        return new AgentDtos.EventResponse(r.eventoId(), r.replay());
    }

    // =================================================================
    // Heartbeat
    // =================================================================

    /**
     * Marca o equipamento como vivo.
     *
     * Quem esta vivo, a rigor, e' o AGENTE — o leitor pode ter caido sem
     * o agente perceber. Ainda assim e' a melhor informacao disponivel:
     * sondar o leitor a partir da nuvem nao funciona (ele esta em
     * 192.168.x) e so' gastaria timeout.
     */
    @Transactional
    public void heartbeat(UUID tenantId, AgentDtos.HeartbeatRequest req) {
        Dispositivo d = dispositivos.findById(req.dispositivoId())
                .filter(x -> tenantId.equals(x.getTenantId()))
                .filter(x -> Boolean.FALSE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipamento não encontrado."));
        Instant agora = Instant.now();
        d.setUltimoHeartbeat(agora);
        d.setUltimoPing(agora);
        d.setOnline(true);
        if (req.agentVersion() != null && !req.agentVersion().isBlank()) {
            d.setAgentVersion(req.agentVersion().length() > 20
                    ? req.agentVersion().substring(0, 20) : req.agentVersion());
        }
        if (req.ultimoLogIdLido() != null) {
            d.setUltimaLeituraLog(agora);
        }
        dispositivos.save(d);
    }

    // =================================================================
    // Fila de tarefas
    // =================================================================

    @Transactional
    public List<AgentDtos.TaskDto> tarefasPendentes(UUID tenantId, UUID dispositivoId) {
        List<AccAgentTask> pendentes = tarefas.buscarPendentes(
                tenantId, StatusAgentTask.PENDENTE, dispositivoId, Instant.now(),
                PageRequest.of(0, 50));
        List<AgentDtos.TaskDto> saida = new ArrayList<>();
        for (AccAgentTask t : pendentes) {
            // Marca ENVIADA na entrega: se o agente morrer antes de
            // responder, a tarefa fica visivel como pendurada em vez de
            // ser reentregue para sempre a cada polling.
            t.setStatus(StatusAgentTask.ENVIADA);
            t.setTentativas(t.getTentativas() + 1);
            tarefas.save(t);
            saida.add(AgentDtos.TaskDto.from(t));
        }
        return saida;
    }

    @Transactional
    public AgentDtos.TaskDto resultado(UUID tenantId, UUID tarefaId, AgentDtos.TaskResultRequest req) {
        AccAgentTask t = tarefas.findByIdAndTenantId(tarefaId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
        t.setStatus(req.sucesso() ? StatusAgentTask.CONCLUIDA : StatusAgentTask.FALHOU);
        t.setResultado(req.resultado());
        t.setConcluidoEm(Instant.now());
        return AgentDtos.TaskDto.from(tarefas.save(t));
    }
}
