package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.dto.OcorrenciaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.OcorrenciaResponse;
import br.com.alfaschool.backend.application.access.retirada.dto.TratativaRequest;
import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.domain.access.retirada.AccOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.StatusOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccOcorrenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD e fluxo de tratamento das ocorrencias, e tambem o ponto de entrada
 * que as outras fatias usam para registrar o que deu errado.
 */
@Service
public class OcorrenciaService implements OcorrenciaRegistroPort {

    private static final Logger log = LoggerFactory.getLogger(OcorrenciaService.class);

    private final AccOcorrenciaRepository ocorrenciaRepository;

    /**
     * ObjectProvider e nao injecao direta porque os modulos sao contratados
     * separadamente: uma escola pode ter ACCESS sem o motor de notificacao.
     * Com dependencia rigida, a ausencia de um modulo impediria a aplicacao
     * inteira de subir.
     */
    private final ObjectProvider<NotificacaoPort> notificacaoPort;

    private final br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository alunoRepository;
    private final br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository userRepository;

    public OcorrenciaService(AccOcorrenciaRepository ocorrenciaRepository,
                             ObjectProvider<NotificacaoPort> notificacaoPort,
                             br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository alunoRepository,
                             br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository userRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.notificacaoPort = notificacaoPort;
        this.alunoRepository = alunoRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolve os nomes de uma pagina inteira em duas consultas, nao em duas
     * por linha. Sao poucos ids distintos: a mesma ocorrencia costuma
     * repetir aluno, e quem trata e' um punhado de pessoas.
     */
    /** Mesma resolucao da listagem, para uma ocorrencia so'. */
    private OcorrenciaResponse comNomes(AccOcorrencia o) {
        String alunoNome = o.getAlunoId() == null ? null
                : alunoRepository.findById(o.getAlunoId()).map(a -> a.getNome()).orElse(null);
        String tratadoPor = o.getTratadoPorUserId() == null ? null
                : userRepository.findById(o.getTratadoPorUserId()).map(u -> u.getName()).orElse(null);
        return OcorrenciaResponse.from(o, alunoNome, tratadoPor);
    }

    private Page<OcorrenciaResponse> comNomes(Page<AccOcorrencia> pagina) {
        java.util.Set<UUID> alunoIds = pagina.getContent().stream()
                .map(AccOcorrencia::getAlunoId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<UUID> userIds = pagina.getContent().stream()
                .map(AccOcorrencia::getTratadoPorUserId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, String> alunos = alunoIds.isEmpty() ? Map.of()
                : alunoRepository.findAllById(alunoIds).stream()
                    .collect(java.util.stream.Collectors.toMap(a -> a.getId(), a -> a.getNome()));
        Map<UUID, String> usuarios = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream()
                    .collect(java.util.stream.Collectors.toMap(u -> u.getId(), u -> u.getName()));

        return pagina.map(o -> OcorrenciaResponse.from(o,
                o.getAlunoId() == null ? null : alunos.get(o.getAlunoId()),
                o.getTratadoPorUserId() == null ? null : usuarios.get(o.getTratadoPorUserId())));
    }

    // ---------------------------------------------------------------
    // Entrada para outras fatias
    // ---------------------------------------------------------------

    /**
     * REQUIRES_NEW: a ocorrencia e' o registro de que algo deu errado. Se
     * ela participasse da transacao do chamador, um rollback la' apagaria
     * justamente a prova do problema.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccOcorrencia registrar(RegistrarOcorrenciaEvent pedido) {
        try {
            AccOcorrencia ocorrencia = new AccOcorrencia();
            ocorrencia.setTenantId(pedido.tenantId());
            ocorrencia.setUnitId(pedido.unitId());
            ocorrencia.setTipo(pedido.tipo());
            ocorrencia.setGravidade(pedido.gravidade() == null ? GravidadeOcorrencia.MEDIA : pedido.gravidade());
            ocorrencia.setAlunoId(pedido.alunoId());
            ocorrencia.setPessoaAutorizadaId(pedido.pessoaAutorizadaId());
            ocorrencia.setDispositivoId(pedido.dispositivoId());
            ocorrencia.setEventoId(pedido.eventoId());
            ocorrencia.setRetiradaId(pedido.retiradaId());
            ocorrencia.setDescricao(pedido.descricao() == null ? pedido.tipo().name() : pedido.descricao());
            ocorrencia.setStatus(StatusOcorrencia.ABERTA);
            ocorrencia.setOcorridoEm(Instant.now());
            AccOcorrencia salva = ocorrenciaRepository.save(ocorrencia);
            notificarCoordenacao(salva);
            return salva;
        } catch (Exception e) {
            // Contrato do port: nunca derruba o chamador. Uma portaria nao
            // pode parar de registrar entradas porque a ocorrencia falhou.
            log.error("Falha ao registrar ocorrencia {} do tenant {}", pedido.tipo(), pedido.tenantId(), e);
            return null;
        }
    }

    /** Mesma entrada, via evento, para quem nao quer depender deste pacote. */
    @EventListener
    public void aoReceberPedido(RegistrarOcorrenciaEvent pedido) {
        registrar(pedido);
    }

    private void notificarCoordenacao(AccOcorrencia ocorrencia) {
        NotificacaoPort notificacao = notificacaoPort.getIfAvailable();
        if (notificacao == null) {
            log.warn("Ocorrencia {} gravada sem aviso: motor de notificacao nao esta disponivel",
                    ocorrencia.getId());
            return;
        }
        try {
            Map<String, String> variaveis = new HashMap<>();
            variaveis.put("tipo", ocorrencia.getTipo().name());
            variaveis.put("gravidade", ocorrencia.getGravidade().name());
            variaveis.put("descricao", ocorrencia.getDescricao());
            // Sem foto, sem biometria, sem dado sensivel: a mensagem so'
            // avisa que existe uma ocorrencia para olhar no sistema.
            notificacao.enfileirar(
                    ocorrencia.getTenantId(),
                    eventoDeNotificacao(ocorrencia.getTipo()),
                    TitularTipo.COLABORADOR,
                    null,
                    ocorrencia.getAlunoId(),
                    variaveis,
                    "OCORRENCIA:" + ocorrencia.getId());
        } catch (Exception e) {
            log.warn("Ocorrencia {} gravada, mas a notificacao falhou", ocorrencia.getId(), e);
        }
    }

    private EventoNotificacao eventoDeNotificacao(TipoOcorrencia tipo) {
        return switch (tipo) {
            case TENTATIVA_NAO_AUTORIZADA, PESSOA_DESCONHECIDA, RESTRICAO_JUDICIAL -> EventoNotificacao.TENTATIVA_NAO_AUTORIZADA;
            case HORARIO_EXCEDIDO -> EventoNotificacao.HORARIO_EXCEDIDO;
            case EQUIPAMENTO_OFFLINE -> EventoNotificacao.EQUIPAMENTO_OFFLINE;
            // Retirada manual, saida sem registro e entrada duplicada nao tem
            // evento proprio no catalogo: entram como alerta de tentativa,
            // que e' a caixa que a coordenacao ja acompanha.
            default -> EventoNotificacao.TENTATIVA_NAO_AUTORIZADA;
        };
    }

    // ---------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------

    @Transactional
    public OcorrenciaResponse criar(OcorrenciaRequest request) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        AccOcorrencia ocorrencia = new AccOcorrencia();
        ocorrencia.setTenantId(tenantId);
        ocorrencia.setUnitId(request.unitId());
        ocorrencia.setTipo(request.tipo());
        ocorrencia.setGravidade(request.gravidade() == null ? GravidadeOcorrencia.MEDIA : request.gravidade());
        ocorrencia.setAlunoId(request.alunoId());
        ocorrencia.setPessoaAutorizadaId(request.pessoaAutorizadaId());
        ocorrencia.setDispositivoId(request.dispositivoId());
        ocorrencia.setRetiradaId(request.retiradaId());
        ocorrencia.setDescricao(request.descricao());
        ocorrencia.setStatus(StatusOcorrencia.ABERTA);
        // Quando a coordenacao nao informa, o fato e' agora — que e' o
        // caso das ocorrencias geradas pelo proprio sistema.
        ocorrencia.setOcorridoEm(request.ocorridoEm() != null ? request.ocorridoEm() : Instant.now());
        ocorrencia.setCreatedBy(ContextoAcesso.userIdOuNulo());
        return comNomes(ocorrenciaRepository.save(ocorrencia));
    }

    public Page<OcorrenciaResponse> listar(UUID unitId,
                                           TipoOcorrencia tipo,
                                           GravidadeOcorrencia gravidade,
                                           StatusOcorrencia status,
                                           String q,
                                           Pageable pageable) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        String termo = (q == null || q.isBlank()) ? null : q.trim();
        return comNomes(ocorrenciaRepository.buscar(tenantId, termo, unitId, tipo, gravidade, status, pageable));
    }

    public OcorrenciaResponse buscar(UUID id) {
        return comNomes(carregar(id));
    }

    @Transactional
    public OcorrenciaResponse atualizar(UUID id, OcorrenciaRequest request) {
        AccOcorrencia ocorrencia = carregar(id);
        if (ocorrencia.getStatus() == StatusOcorrencia.FECHADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ocorrencia ja fechada");
        }
        ocorrencia.setUnitId(request.unitId());
        ocorrencia.setTipo(request.tipo());
        ocorrencia.setGravidade(request.gravidade() == null ? ocorrencia.getGravidade() : request.gravidade());
        ocorrencia.setDescricao(request.descricao());
        ocorrencia.setUpdatedBy(ContextoAcesso.userIdOuNulo());
        return comNomes(ocorrenciaRepository.save(ocorrencia));
    }

    /**
     * Assumir a ocorrencia. Exige usuario: ocorrencia tratada por ninguem e'
     * o mesmo que ocorrencia nao tratada.
     */
    @Transactional
    public OcorrenciaResponse tratar(UUID id, TratativaRequest request) {
        AccOcorrencia ocorrencia = carregar(id);
        if (ocorrencia.getStatus() == StatusOcorrencia.FECHADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ocorrencia ja fechada");
        }
        if (request == null || request.tratativa() == null || request.tratativa().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a tratativa");
        }
        ocorrencia.setTratadoPorUserId(ContextoAcesso.userIdObrigatorio());
        ocorrencia.setTratadoEm(Instant.now());
        ocorrencia.setTratativa(request.tratativa());
        ocorrencia.setStatus(StatusOcorrencia.EM_TRATATIVA);
        ocorrencia.setUpdatedBy(ocorrencia.getTratadoPorUserId());
        return comNomes(ocorrenciaRepository.save(ocorrencia));
    }

    /** So' fecha o que passou pela tratativa — senao some da tela sem ninguem ter olhado. */
    @Transactional
    public OcorrenciaResponse fechar(UUID id) {
        AccOcorrencia ocorrencia = carregar(id);
        if (ocorrencia.getStatus() == StatusOcorrencia.FECHADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ocorrencia ja fechada");
        }
        if (ocorrencia.getTratadoPorUserId() == null || ocorrencia.getTratativa() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Registre a tratativa antes de fechar a ocorrencia");
        }
        ocorrencia.setStatus(StatusOcorrencia.FECHADA);
        ocorrencia.setUpdatedBy(ContextoAcesso.userIdObrigatorio());
        return comNomes(ocorrenciaRepository.save(ocorrencia));
    }

    @Transactional
    public void remover(UUID id) {
        AccOcorrencia ocorrencia = carregar(id);
        ocorrencia.setDeleted(true);
        ocorrencia.setUpdatedBy(ContextoAcesso.userIdOuNulo());
        ocorrenciaRepository.save(ocorrencia);
    }

    private AccOcorrencia carregar(UUID id) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return ocorrenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrencia nao encontrada"));
    }
}
