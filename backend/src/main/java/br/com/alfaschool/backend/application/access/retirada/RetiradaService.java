package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.dto.EntregaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RegistrarSaidaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaManualRequest;
import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.application.access.shared.AutorizacaoPort;
import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.application.access.shared.PermanenciaPort;
import br.com.alfaschool.backend.domain.access.retirada.AccRetirada;
import br.com.alfaschool.backend.domain.access.retirada.AccRetiradaHistorico;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.MaquinaEstadosRetirada;
import br.com.alfaschool.backend.domain.access.retirada.OrigemTransicao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRetiradaHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRetiradaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Coracao da fila de retirada.
 *
 * A regra que este service existe para proteger: reconhecer o rosto do
 * responsavel NAO entrega o aluno e NAO encerra a permanencia. Sao tres
 * momentos com tres carimbos de tempo, e este service e' o unico lugar que
 * pode move-los.
 */
@Service
public class RetiradaService {

    private static final Logger log = LoggerFactory.getLogger(RetiradaService.class);

    /** Status em que a retirada ainda ocupa lugar na fila. */
    public static final Set<StatusRetirada> EM_ABERTO =
            EnumSet.of(StatusRetirada.SOLICITADA, StatusRetirada.PREPARANDO, StatusRetirada.PRONTO);

    private final AccRetiradaRepository retiradaRepository;
    private final AccRetiradaHistoricoRepository historicoRepository;
    /**
     * Os tres ports abaixo pertencem a outras fatias e chegam por
     * ObjectProvider, nao por injecao rigida: os modulos sao contratados
     * separadamente e a ausencia de um deles nao pode impedir a aplicacao
     * inteira de subir.
     *
     * Cada um degrada de um jeito diferente, conforme o proprio contrato:
     * autorizacao FALHA FECHADA (sem ela, ninguem retira ninguem),
     * permanencia recusa a operacao em voz alta (503) e notificacao apenas
     * deixa de avisar.
     */
    private final ObjectProvider<AutorizacaoPort> autorizacaoPort;
    private final AlunosAutorizadosPort alunosAutorizadosPort;
    private final ContextoAlunoPort contextoAlunoPort;
    private final ObjectProvider<PermanenciaPort> permanenciaPort;
    private final ObjectProvider<NotificacaoPort> notificacaoPort;
    private final OcorrenciaRegistroPort ocorrenciaPort;
    private final ApplicationEventPublisher publisher;

    public RetiradaService(AccRetiradaRepository retiradaRepository,
                           AccRetiradaHistoricoRepository historicoRepository,
                           ObjectProvider<AutorizacaoPort> autorizacaoPort,
                           AlunosAutorizadosPort alunosAutorizadosPort,
                           ContextoAlunoPort contextoAlunoPort,
                           ObjectProvider<PermanenciaPort> permanenciaPort,
                           ObjectProvider<NotificacaoPort> notificacaoPort,
                           OcorrenciaRegistroPort ocorrenciaPort,
                           ApplicationEventPublisher publisher) {
        this.retiradaRepository = retiradaRepository;
        this.historicoRepository = historicoRepository;
        this.autorizacaoPort = autorizacaoPort;
        this.alunosAutorizadosPort = alunosAutorizadosPort;
        this.contextoAlunoPort = contextoAlunoPort;
        this.permanenciaPort = permanenciaPort;
        this.notificacaoPort = notificacaoPort;
        this.ocorrenciaPort = ocorrenciaPort;
        this.publisher = publisher;
    }

    // =================================================================
    // 1. ABRIR — chegada do responsavel na portaria
    // =================================================================

    /**
     * Uma leitura no leitor de responsavel abre uma retirada para CADA aluno
     * que aquela pessoa pode retirar naquele instante.
     *
     * Pessoa reconhecida que nao pode retirar ninguem NAO gera retirada:
     * gera ocorrencia e vai para a mesa da coordenacao. Abrir a fila para
     * quem nao tem autorizacao seria transformar um alarme em rotina.
     */
    @Transactional
    public List<AccRetirada> abrirPorReconhecimento(AcessoRegistradoEvent evento) {
        if (evento == null || !evento.chegadaDeResponsavel()) {
            return List.of();
        }
        UUID tenantId = evento.tenantId();
        UUID pessoaId = evento.titularId();
        Instant momento = evento.dataHora() == null ? Instant.now() : evento.dataHora();

        if (tenantId == null || pessoaId == null) {
            return List.of();
        }

        // Pessoa que o leitor nao reconheceu: nao ha' o que autorizar, so'
        // o que registrar.
        if (!evento.permitido() || evento.titularTipo() == TitularTipo.DESCONHECIDO) {
            registrarOcorrenciaDeTentativa(evento, TipoOcorrencia.PESSOA_DESCONHECIDA,
                    GravidadeOcorrencia.ALTA,
                    "Leitura de pessoa nao reconhecida na portaria");
            return List.of();
        }

        // Falha fechada: sem o modulo de autorizacao no ar, nenhuma retirada
        // e' aberta. O certo aqui e' a fila parar, nunca a porta abrir.
        AutorizacaoPort autorizacao = autorizacaoPort.getIfAvailable();
        if (autorizacao == null) {
            log.error("Modulo de autorizacao indisponivel: nenhuma retirada sera aberta (tenant {})", tenantId);
            registrarOcorrenciaDeTentativa(evento, TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA,
                    GravidadeOcorrencia.CRITICA,
                    "Modulo de autorizacao indisponivel: retirada nao pode ser liberada");
            return List.of();
        }

        List<UUID> candidatos = alunosAutorizadosPort.alunosCandidatos(tenantId, pessoaId, momento);
        List<AccRetirada> abertas = new ArrayList<>();
        Integer ordem = null;

        for (UUID alunoId : candidatos) {
            // Palavra final e' sempre do AutorizacaoPort: vigencia, faixa de
            // horario e restricao judicial moram la'.
            AutorizacaoPort.Veredito veredito = autorizacao.verificar(alunoId, pessoaId, momento);
            if (veredito == null || !veredito.permitido()) {
                continue;
            }
            if (jaTemRetiradaAberta(tenantId, alunoId)) {
                // O pai encostou o rosto duas vezes. A fila nao pode ganhar
                // um cartao duplicado por causa disso.
                continue;
            }
            ContextoAlunoPort.ContextoAluno contexto = contextoAlunoPort.contextoDe(tenantId, alunoId, momento);
            UUID unitId = evento.unitId() != null ? evento.unitId() : contexto.unitId();
            if (ordem == null) {
                // A pessoa chegou UMA vez. Os irmaos entram na mesma posicao
                // da fila: quem chegou depois nao pode passar na frente
                // porque o primeiro tinha dois filhos.
                ordem = proximaOrdemChegada(tenantId, unitId, momento);
            }

            AccRetirada retirada = new AccRetirada();
            retirada.setTenantId(tenantId);
            retirada.setUnitId(unitId);
            retirada.setAlunoId(alunoId);
            retirada.setPessoaAutorizadaId(pessoaId);
            retirada.setAutorizacaoId(veredito.autorizacaoId());
            retirada.setPortariaId(evento.portariaId());
            retirada.setDispositivoId(evento.dispositivoId());
            retirada.setTurmaId(contexto.turmaId());
            retirada.setSalaId(contexto.salaId());
            retirada.setStatus(StatusRetirada.SOLICITADA);
            retirada.setOrdemChegada(ordem);
            retirada.setSolicitadoEm(momento);
            retirada.setSolicitacaoEventoId(evento.eventoId());
            AccRetirada salva = retiradaRepository.save(retirada);

            gravarHistorico(salva, null, StatusRetirada.SOLICITADA, null, OrigemTransicao.CATRACA, null, null);
            publicarMudanca(salva, null);
            avisarFamilia(salva);
            abertas.add(salva);
        }

        if (abertas.isEmpty()) {
            registrarOcorrenciaDeTentativa(evento, TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA,
                    GravidadeOcorrencia.ALTA,
                    "Pessoa reconhecida na portaria sem autorizacao vigente para retirar nenhum aluno");
        }
        return abertas;
    }

    /**
     * Coordenacao abre sem leitura biometrica. Sempre gera ocorrencia: e' o
     * caminho que contorna o controle biometrico e por isso precisa deixar
     * rastro para auditoria, mesmo quando e' legitimo.
     */
    @Transactional
    public AccRetirada abrirManual(RetiradaManualRequest request, String ip) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        UUID userId = ContextoAcesso.userIdObrigatorio();
        if (request.motivo() == null || request.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o motivo da retirada manual");
        }
        if (request.pessoaAutorizadaId() == null
                && (request.retiradoPorNome() == null || request.retiradoPorNome().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe quem esta retirando o aluno");
        }
        if (jaTemRetiradaAberta(tenantId, request.alunoId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe uma retirada em aberto para este aluno");
        }

        Instant agora = Instant.now();
        ContextoAlunoPort.ContextoAluno contexto = contextoAlunoPort.contextoDe(tenantId, request.alunoId(), agora);
        UUID unitId = request.unitId() != null ? request.unitId() : contexto.unitId();

        AccRetirada retirada = new AccRetirada();
        retirada.setTenantId(tenantId);
        retirada.setUnitId(unitId);
        retirada.setAlunoId(request.alunoId());
        retirada.setPessoaAutorizadaId(request.pessoaAutorizadaId());
        retirada.setPortariaId(request.portariaId());
        retirada.setTurmaId(contexto.turmaId());
        retirada.setSalaId(contexto.salaId());
        retirada.setStatus(StatusRetirada.SOLICITADA);
        retirada.setOrdemChegada(proximaOrdemChegada(tenantId, unitId, agora));
        retirada.setSolicitadoEm(agora);
        retirada.setRetiradaManual(true);
        retirada.setMotivo(request.motivo());
        // Sem coluna propria para o nome/documento de quem retira quando a
        // pessoa nao esta' cadastrada: fica na observacao ate' o schema ter
        // retirado_por_nome / retirado_por_documento.
        retirada.setObservacao(montarObservacaoManual(request));
        retirada.setCreatedBy(userId);
        AccRetirada salva = retiradaRepository.save(retirada);

        gravarHistorico(salva, null, StatusRetirada.SOLICITADA, userId, OrigemTransicao.ADMIN, request.motivo(), ip);
        publicarMudanca(salva, null);

        ocorrenciaPort.registrar(new RegistrarOcorrenciaEvent(
                tenantId, unitId, TipoOcorrencia.RETIRADA_MANUAL, GravidadeOcorrencia.MEDIA,
                salva.getAlunoId(), salva.getPessoaAutorizadaId(), null, null, salva.getId(),
                "Retirada manual aberta pela coordenacao: " + request.motivo()));
        return salva;
    }

    private String montarObservacaoManual(RetiradaManualRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.retiradoPorNome() != null && !request.retiradoPorNome().isBlank()) {
            sb.append("Retirado por: ").append(request.retiradoPorNome().trim());
        }
        if (request.retiradoPorDocumento() != null && !request.retiradoPorDocumento().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Documento: ").append(request.retiradoPorDocumento().trim());
        }
        if (request.observacao() != null && !request.observacao().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(request.observacao().trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // =================================================================
    // 2. TRANSICOES
    // =================================================================

    /** Professora tocou no painel da sala. Exige usuario autenticado. */
    @Transactional
    public AccRetirada preparar(UUID id, String ip) {
        UUID userId = ContextoAcesso.userIdObrigatorio();
        AccRetirada retirada = carregar(id);
        StatusRetirada anterior = retirada.getStatus();
        aplicarTransicao(retirada, StatusRetirada.PREPARANDO, userId, OrigemTransicao.PAINEL, null, ip);
        retirada.setPreparandoEm(Instant.now());
        retirada.setPreparadoPorUserId(userId);
        return salvarEPublicar(retirada, anterior);
    }

    /**
     * Preparo disparado pela TV da sala, autenticada por token de
     * dispositivo em vez de login.
     *
     * Por que isto existe: a Smart TV fica ligada o dia inteiro, nao tem
     * teclado e e' compartilhada pela turma. Exigir login ali tornaria o
     * botao inutilizavel na pratica — e "preparar" nao e' o ato de
     * responsabilidade. O ato de responsabilidade e' ENTREGAR, que continua
     * exigindo colaborador identificado, sempre.
     *
     * Como nao ha usuario, o historico guarda origem PAINEL e, no motivo, a
     * identificacao do painel e da TV que tocaram no botao. O campo
     * preparado_por_user_id fica nulo de proposito: inventar um usuario ali
     * seria pior do que assumir que a acao veio de um dispositivo.
     *
     * @param identificacaoPainel texto que identifica painel e TV, para a trilha
     */
    @Transactional
    public AccRetirada prepararPeloPainel(UUID id, String identificacaoPainel, String ip) {
        AccRetirada retirada = carregar(id);
        StatusRetirada anterior = retirada.getStatus();
        aplicarTransicao(retirada, StatusRetirada.PREPARANDO, null, OrigemTransicao.PAINEL,
                identificacaoPainel, ip);
        retirada.setPreparandoEm(Instant.now());
        return salvarEPublicar(retirada, anterior);
    }

    /** Aluno pronto para descer. */
    @Transactional
    public AccRetirada pronto(UUID id, String ip) {
        UUID userId = ContextoAcesso.userIdObrigatorio();
        AccRetirada retirada = carregar(id);
        StatusRetirada anterior = retirada.getStatus();
        aplicarTransicao(retirada, StatusRetirada.PRONTO, userId, OrigemTransicao.PAINEL, null, ip);
        retirada.setProntoEm(Instant.now());
        return salvarEPublicar(retirada, anterior);
    }

    /**
     * O ato de responsabilidade: alguem confirma que colocou a crianca na
     * mao do responsavel.
     *
     * NAO chama PermanenciaPort.registrarSaida. Entregar nao e' sair: a
     * crianca ainda esta' na escola ate' cruzar a catraca de saida. Quem
     * encerra a permanencia e' o evento de saida (fatia de permanencia) ou,
     * em escola sem catraca de saida, o endpoint explicito
     * POST /retiradas/{id}/registrar-saida.
     */
    @Transactional
    public AccRetirada entregar(UUID id, EntregaRequest request, String ip) {
        // Primeiro a identificacao, depois o resto: ninguem entrega crianca
        // anonimamente, nem por engano de configuracao.
        UUID userId = ContextoAcesso.userIdObrigatorio();
        AccRetirada retirada = carregar(id);
        StatusRetirada anterior = retirada.getStatus();
        aplicarTransicao(retirada, StatusRetirada.ENTREGUE, userId, OrigemTransicao.PAINEL, null, ip);
        retirada.setEntregueEm(Instant.now());
        retirada.setEntreguePorUserId(userId);
        if (request != null && request.observacao() != null && !request.observacao().isBlank()) {
            retirada.setObservacao(request.observacao());
        }
        return salvarEPublicar(retirada, anterior);
    }

    @Transactional
    public AccRetirada cancelar(UUID id, String motivo, String ip) {
        UUID userId = ContextoAcesso.userIdObrigatorio();
        exigirMotivo(motivo);
        AccRetirada retirada = carregar(id);
        StatusRetirada anterior = retirada.getStatus();
        aplicarTransicao(retirada, StatusRetirada.CANCELADA, userId, OrigemTransicao.PAINEL, motivo, ip);
        retirada.setCanceladoEm(Instant.now());
        retirada.setCanceladoPorUserId(userId);
        retirada.setMotivo(motivo);
        return salvarEPublicar(retirada, anterior);
    }

    @Transactional
    public AccRetirada negar(UUID id, String motivo, String ip) {
        UUID userId = ContextoAcesso.userIdObrigatorio();
        exigirMotivo(motivo);
        AccRetirada retirada = carregar(id);
        StatusRetirada anterior = retirada.getStatus();
        aplicarTransicao(retirada, StatusRetirada.NEGADA, userId, OrigemTransicao.PAINEL, motivo, ip);
        retirada.setCanceladoEm(Instant.now());
        retirada.setCanceladoPorUserId(userId);
        retirada.setMotivo(motivo);
        AccRetirada salva = salvarEPublicar(retirada, anterior);

        ocorrenciaPort.registrar(new RegistrarOcorrenciaEvent(
                salva.getTenantId(), salva.getUnitId(), TipoOcorrencia.TENTATIVA_NAO_AUTORIZADA,
                GravidadeOcorrencia.ALTA, salva.getAlunoId(), salva.getPessoaAutorizadaId(),
                null, null, salva.getId(), "Retirada negada na portaria: " + motivo));
        return salva;
    }

    /**
     * UNICO ponto do modulo que encerra a permanencia a mando de um humano.
     *
     * Existe para a escola que nao tem catraca de saida. Onde ha' catraca, a
     * saida chega pelo evento de leitura e este endpoint nao deve ser usado.
     */
    @Transactional
    public AccRetirada registrarSaida(UUID id, RegistrarSaidaRequest request, String ip) {
        UUID userId = ContextoAcesso.userIdObrigatorio();
        AccRetirada retirada = carregar(id);
        if (retirada.getStatus() != StatusRetirada.ENTREGUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "So e possivel registrar a saida de uma retirada ja entregue");
        }
        if (retirada.getSaidaEm() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saida ja registrada para esta retirada");
        }
        Instant momento = (request != null && request.momento() != null) ? request.momento() : Instant.now();
        if (retirada.getEntregueEm() != null && momento.isBefore(retirada.getEntregueEm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A saida nao pode ser anterior a entrega");
        }
        // Sem o modulo de permanencia no ar a saida NAO e' registrada pela
        // metade: melhor recusar em voz alta do que deixar a coordenacao
        // achando que a permanencia foi encerrada. A checagem vem ANTES de
        // tocar na entidade — nada de objeto sujo em memoria.
        PermanenciaPort permanencia = permanenciaPort.getIfAvailable();
        if (permanencia == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Modulo de permanencia indisponivel: nao foi possivel registrar a saida");
        }

        retirada.setSaidaEm(momento);
        if (request != null && request.observacao() != null && !request.observacao().isBlank()) {
            retirada.setObservacao(request.observacao());
        }
        retirada.setUpdatedBy(userId);
        AccRetirada salva = retiradaRepository.save(retirada);

        // A permanencia so' fecha aqui. Nao ha' mudanca de status: ENTREGUE
        // continua sendo o estado final da fila.
        permanencia.registrarSaida(salva.getTenantId(), salva.getAlunoId(), momento, null);
        gravarHistorico(salva, StatusRetirada.ENTREGUE, StatusRetirada.ENTREGUE, userId,
                OrigemTransicao.ADMIN, "Saida efetiva registrada manualmente", ip);
        return salva;
    }

    /**
     * Saida vinda da catraca: carimba saida_em na retirada entregue do dia.
     *
     * Aqui NAO se chama registrarSaida: o evento de leitura ja' e' consumido
     * pela fatia de permanencia, que e' a dona desse calculo. Chamar de novo
     * fecharia a permanencia duas vezes.
     */
    @Transactional
    public void marcarSaidaPorEvento(AcessoRegistradoEvent evento) {
        if (evento == null || evento.titularId() == null) {
            return;
        }
        List<AccRetirada> candidatas = retiradaRepository
                .findByTenantIdAndAlunoIdAndDeletedFalseOrderBySolicitadoEmDesc(evento.tenantId(), evento.titularId());
        for (AccRetirada retirada : candidatas) {
            if (retirada.getStatus() == StatusRetirada.ENTREGUE && retirada.getSaidaEm() == null) {
                retirada.setSaidaEm(evento.dataHora());
                retirada.setSaidaEventoId(evento.eventoId());
                retiradaRepository.save(retirada);
                return;
            }
        }
    }

    // =================================================================
    // Infra interna
    // =================================================================

    /**
     * Ponto unico de validacao. Todo caminho de mudanca de status passa por
     * aqui — se um dia alguem adicionar um estado novo, e' este mapa que
     * muda, e nao vinte ifs espalhados.
     */
    private void aplicarTransicao(AccRetirada retirada,
                                  StatusRetirada novo,
                                  UUID userId,
                                  OrigemTransicao origem,
                                  String motivo,
                                  String ip) {
        StatusRetirada atual = retirada.getStatus();
        String erro = MaquinaEstadosRetirada.validarTransicao(atual, novo);
        if (erro != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, erro);
        }
        retirada.setStatus(novo);
        retirada.setUpdatedBy(userId);
        gravarHistorico(retirada, atual, novo, userId, origem, motivo, ip);
    }

    private AccRetirada salvarEPublicar(AccRetirada retirada, StatusRetirada anterior) {
        AccRetirada salva = retiradaRepository.save(retirada);
        publicarMudanca(salva, anterior);
        return salva;
    }

    private void publicarMudanca(AccRetirada retirada, StatusRetirada anterior) {
        publisher.publishEvent(new RetiradaStatusMudouEvent(
                retirada.getTenantId(),
                retirada.getId(),
                retirada.getUnitId(),
                retirada.getAlunoId(),
                retirada.getTurmaId(),
                retirada.getSalaId(),
                retirada.getPortariaId(),
                anterior,
                retirada.getStatus()));
    }

    private void gravarHistorico(AccRetirada retirada,
                                 StatusRetirada anterior,
                                 StatusRetirada novo,
                                 UUID userId,
                                 OrigemTransicao origem,
                                 String motivo,
                                 String ip) {
        AccRetiradaHistorico h = new AccRetiradaHistorico();
        h.setTenantId(retirada.getTenantId());
        h.setRetiradaId(retirada.getId());
        h.setStatusAnterior(anterior);
        h.setStatusNovo(novo);
        h.setUserId(userId);
        h.setOrigem(origem);
        h.setMotivo(motivo);
        h.setIp(ip);
        h.setCreatedAt(Instant.now());
        historicoRepository.save(h);
    }

    private boolean jaTemRetiradaAberta(UUID tenantId, UUID alunoId) {
        return !retiradaRepository.abertasDoAluno(tenantId, alunoId, EM_ABERTO).isEmpty();
    }

    private Integer proximaOrdemChegada(UUID tenantId, UUID unitId, Instant momento) {
        LocalDate dia = LocalDate.ofInstant(momento, RetiradaLookupJdbc.ZONA);
        Instant inicio = dia.atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant();
        Instant fim = dia.plusDays(1).atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant();
        return retiradaRepository.maiorOrdemChegadaDoDia(tenantId, unitId, inicio, fim) + 1;
    }

    private void exigirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o motivo");
        }
    }

    private AccRetirada carregar(UUID id) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return retiradaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Retirada nao encontrada"));
    }

    private void registrarOcorrenciaDeTentativa(AcessoRegistradoEvent evento,
                                                TipoOcorrencia tipo,
                                                GravidadeOcorrencia gravidade,
                                                String descricao) {
        ocorrenciaPort.registrar(new RegistrarOcorrenciaEvent(
                evento.tenantId(), evento.unitId(), tipo, gravidade,
                null, evento.titularId(), evento.dispositivoId(), evento.eventoId(), null,
                descricao));
    }

    /** Aviso de que o responsavel chegou. Sem foto e sem biometria. */
    private void avisarFamilia(AccRetirada retirada) {
        NotificacaoPort notificacao = notificacaoPort.getIfAvailable();
        if (notificacao == null) {
            return;
        }
        try {
            Map<String, String> variaveis = new HashMap<>();
            variaveis.put("ordemChegada", String.valueOf(retirada.getOrdemChegada()));
            notificacao.enfileirar(
                    retirada.getTenantId(),
                    EventoNotificacao.RETIRADA_SOLICITADA,
                    TitularTipo.ALUNO,
                    retirada.getAlunoId(),
                    retirada.getAlunoId(),
                    variaveis,
                    "RETIRADA_SOLICITADA:" + retirada.getId());
        } catch (Exception e) {
            log.warn("Retirada {} aberta, mas a notificacao falhou", retirada.getId(), e);
        }
    }
}
