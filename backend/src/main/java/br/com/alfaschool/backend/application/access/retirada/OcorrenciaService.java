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

    public OcorrenciaService(AccOcorrenciaRepository ocorrenciaRepository,
                             ObjectProvider<NotificacaoPort> notificacaoPort) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.notificacaoPort = notificacaoPort;
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
        ocorrencia.setCreatedBy(ContextoAcesso.userIdOuNulo());
        return OcorrenciaResponse.from(ocorrenciaRepository.save(ocorrencia));
    }

    public Page<OcorrenciaResponse> listar(UUID unitId,
                                           TipoOcorrencia tipo,
                                           GravidadeOcorrencia gravidade,
                                           StatusOcorrencia status,
                                           Pageable pageable) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return ocorrenciaRepository.buscar(tenantId, unitId, tipo, gravidade, status, pageable)
                .map(OcorrenciaResponse::from);
    }

    public OcorrenciaResponse buscar(UUID id) {
        return OcorrenciaResponse.from(carregar(id));
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
        return OcorrenciaResponse.from(ocorrenciaRepository.save(ocorrencia));
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
        return OcorrenciaResponse.from(ocorrenciaRepository.save(ocorrencia));
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
        return OcorrenciaResponse.from(ocorrenciaRepository.save(ocorrencia));
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
