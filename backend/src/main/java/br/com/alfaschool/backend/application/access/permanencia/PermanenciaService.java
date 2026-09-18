package br.com.alfaschool.backend.application.access.permanencia;

import br.com.alfaschool.backend.application.access.jornada.JornadaDoDia;
import br.com.alfaschool.backend.application.access.jornada.JornadaService;
import br.com.alfaschool.backend.application.access.permanencia.EventoAcessoLeitor.EventoAcesso;
import br.com.alfaschool.backend.application.access.permanencia.dto.*;
import br.com.alfaschool.backend.application.access.shared.CalendarioPort;
import br.com.alfaschool.backend.application.access.shared.PermanenciaPort;
import br.com.alfaschool.backend.domain.access.permanencia.AccFechamento;
import br.com.alfaschool.backend.domain.access.permanencia.AccPresenca;
import br.com.alfaschool.backend.domain.access.permanencia.AccPresencaPar;
import br.com.alfaschool.backend.domain.access.permanencia.OrigemPar;
import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;
import br.com.alfaschool.backend.domain.shared.AuditLog;
import br.com.alfaschool.backend.infrastructure.persistence.repository.*;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Motor de apuracao de permanencia.
 *
 * <h2>Reconstrucao, nao acumulacao</h2>
 * {@link #recalcularDia} joga fora os pares do dia e os refaz a partir de
 * acc_eventos. E' o que torna a operacao idempotente: rodar uma ou dez
 * vezes da o mesmo resultado, e um evento que chegou atrasado (agente
 * offline que sincronizou depois) entra no lugar certo da fila em vez de
 * virar um par solto no fim do dia.
 *
 * <h2>Tres barreiras que este servico nunca atravessa</h2>
 * <ol>
 *   <li><b>Presenca congelada nao e' recalculada.</b> Fatura emitida nao
 *       muda, nem que a jornada seja editada depois.</li>
 *   <li><b>Dia AJUSTADO a mao nao e' reconstruido automaticamente.</b>
 *       Seria destruir a correcao de um humano com um job de 10 minutos.
 *       So' o recalculo forcado, com flag explicita, passa por cima.</li>
 *   <li><b>Dia INCONSISTENTE nao entra em total nenhum.</b> Os derivados
 *       saem zerados e as consultas agregadas ainda excluem o status.</li>
 * </ol>
 */
@Service
public class PermanenciaService implements PermanenciaPort {

    private static final Logger log = LoggerFactory.getLogger(PermanenciaService.class);

    /** Resultado de uma tentativa de reconstrucao — alimenta os contadores dos jobs. */
    public enum ResultadoRecalculo { RECALCULADO, IGNORADO_CONGELADO, IGNORADO_AJUSTADO, IGNORADO_FECHAMENTO, SEM_DADOS }

    private final AccPresencaRepository presencaRepository;
    private final AccPresencaParRepository parRepository;
    private final AccFechamentoRepository fechamentoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AuditLogRepository auditLogRepository;
    private final JornadaService jornadaService;
    private final EventoAcessoLeitor eventoLeitor;
    /**
     * O calendario letivo e' de outra fatia do modulo. Com ObjectProvider
     * o motor sobe mesmo antes de ela existir, e sem calendario o dia e'
     * tratado como letivo — o previsto contratado continua valendo, que e'
     * o comportamento conservador para quem paga.
     */
    private final ObjectProvider<CalendarioPort> calendarioProvider;
    private final Clock clock;

    /**
     * Construtor do Spring. O @Autowired e' obrigatorio: com dois
     * construtores publicos o container nao escolhe sozinho e o contexto
     * nem sobe.
     */
    @Autowired
    public PermanenciaService(AccPresencaRepository presencaRepository,
                              AccPresencaParRepository parRepository,
                              AccFechamentoRepository fechamentoRepository,
                              MatriculaRepository matriculaRepository,
                              AuditLogRepository auditLogRepository,
                              JornadaService jornadaService,
                              EventoAcessoLeitor eventoLeitor,
                              ObjectProvider<CalendarioPort> calendarioProvider) {
        this(presencaRepository, parRepository, fechamentoRepository, matriculaRepository,
                auditLogRepository, jornadaService, eventoLeitor, calendarioProvider,
                Clock.system(CalculoPermanencia.ZONE));
    }

    /** Construtor com relogio injetado — usado nos testes de borda de data. */
    public PermanenciaService(AccPresencaRepository presencaRepository,
                              AccPresencaParRepository parRepository,
                              AccFechamentoRepository fechamentoRepository,
                              MatriculaRepository matriculaRepository,
                              AuditLogRepository auditLogRepository,
                              JornadaService jornadaService,
                              EventoAcessoLeitor eventoLeitor,
                              ObjectProvider<CalendarioPort> calendarioProvider,
                              Clock clock) {
        this.presencaRepository = presencaRepository;
        this.parRepository = parRepository;
        this.fechamentoRepository = fechamentoRepository;
        this.matriculaRepository = matriculaRepository;
        this.auditLogRepository = auditLogRepository;
        this.jornadaService = jornadaService;
        this.eventoLeitor = eventoLeitor;
        this.calendarioProvider = calendarioProvider;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ porta

    /**
     * A entrada nao e' acumulada num estado: o evento ja esta em
     * acc_eventos, entao reconstruir o dia inteiro da o mesmo resultado e
     * ainda conserta a ordem se um evento antigo chegou depois.
     */
    @Override
    @Transactional
    public void registrarEntrada(UUID tenantId, UUID alunoId, Instant momento, UUID eventoId) {
        recalcularDia(tenantId, alunoId, diaDe(momento));
    }

    /**
     * REGRA INVIOLAVEL: so' chega aqui a SAIDA EFETIVA. Reconhecer o
     * responsavel na portaria ou confirmar a entrega nao encerra
     * permanencia — quem chama isso e' o modulo de eventos, com o evento
     * de passagem do aluno.
     */
    @Override
    @Transactional
    public void registrarSaida(UUID tenantId, UUID alunoId, Instant momento, UUID eventoId) {
        recalcularDia(tenantId, alunoId, diaDe(momento));
    }

    @Override
    @Transactional
    public void recalcularDia(UUID tenantId, UUID alunoId, LocalDate dia) {
        reconstruir(tenantId, alunoId, dia, false);
    }

    /**
     * Reconstroi o dia do aluno a partir dos eventos.
     *
     * @param forcar passa por cima do dia AJUSTADO a mao (e descarta os
     *               pares manuais dele). NUNCA passa por cima de dia
     *               congelado — nao existe flag para isso.
     */
    @Transactional
    public ResultadoRecalculo reconstruir(UUID tenantId, UUID alunoId, LocalDate dia, boolean forcar) {
        AccPresenca presenca = presencaRepository
                .findByTenantIdAndAlunoIdAndDataAndDeletedFalse(tenantId, alunoId, dia)
                .orElse(null);

        if (presenca != null && presenca.isCongelada()) {
            log.info("Permanencia congelada: dia {} do aluno {} nao recalculado (fechamento ja emitido)", dia, alunoId);
            return ResultadoRecalculo.IGNORADO_CONGELADO;
        }
        if (presenca != null && presenca.getStatus() == StatusPresenca.AJUSTADA && !forcar) {
            log.debug("Dia {} do aluno {} tem ajuste manual: recalculo automatico ignorado", dia, alunoId);
            return ResultadoRecalculo.IGNORADO_AJUSTADO;
        }

        List<EventoAcesso> eventos = eventoLeitor.eventosDoDia(tenantId, alunoId, dia);
        if (eventos.isEmpty() && presenca == null) {
            // Sem evento e sem linha previa nao ha o que registrar. Criar uma
            // presenca vazia para todo aluno em todo dia encheria a tabela de
            // linhas que nao significam nada.
            return ResultadoRecalculo.SEM_DADOS;
        }

        UUID unitId = eventos.stream()
                .map(EventoAcesso::unitId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(presenca == null ? null : presenca.getUnitId());

        if (periodoFechado(tenantId, unitId, dia)) {
            log.info("Competencia fechada cobre {} (unit {}): dia do aluno {} nao recalculado", dia, unitId, alunoId);
            return ResultadoRecalculo.IGNORADO_FECHAMENTO;
        }

        JornadaDoDia contrato = jornadaService.resolverDoDia(tenantId, alunoId, dia);
        boolean diaLetivo = ehDiaLetivo(tenantId, unitId, dia);
        CalculoPermanencia.ParametrosDia parametros = new CalculoPermanencia.ParametrosDia(
                diaLetivo, contrato.frequenta(), contrato.entradaPrevista(), contrato.saidaPrevista(),
                contrato.cargaMinutos(), contrato.toleranciaEntradaMin(), contrato.toleranciaSaidaMin(),
                contrato.regraExcedente());

        CalculoPermanencia.ResultadoDia resultado =
                CalculoPermanencia.apurar(eventos, parametros, dia, hoje());

        if (presenca == null) {
            presenca = new AccPresenca();
            presenca.setTenantId(tenantId);
            presenca.setAlunoId(alunoId);
            presenca.setData(dia);
        }
        boolean descartarManuais = forcar && presenca.getStatus() == StatusPresenca.AJUSTADA;
        presenca.setUnitId(unitId);
        presenca.setJornadaId(contrato.jornadaId());
        presenca.setDiaLetivo(diaLetivo);
        aplicarTotais(presenca, resultado.status(), resultado.totais());
        presenca = presencaRepository.save(presenca);

        regravarPares(tenantId, presenca.getId(), resultado.intervalos(), descartarManuais);

        log.debug("Dia {} do aluno {} recalculado: status={} permanencia={}min previsto={}min excedente={}min{}",
                dia, alunoId, presenca.getStatus(), presenca.getMinutosPermanencia(),
                presenca.getMinutosPrevistos(), presenca.getMinutosExcedente(),
                contrato.origemExcecao() ? " (excecao pontual aplicada)" : "");
        return ResultadoRecalculo.RECALCULADO;
    }

    private void aplicarTotais(AccPresenca presenca, StatusPresenca status, CalculoPermanencia.Totais t) {
        presenca.setStatus(status);
        presenca.setPrimeiraEntradaEm(t.primeiraEntrada());
        presenca.setUltimaSaidaEm(t.ultimaSaida());
        presenca.setMinutosPermanencia(t.minutosPermanencia());
        presenca.setMinutosPrevistos(t.minutosPrevistos());
        presenca.setMinutosExcedente(t.minutosExcedente());
        presenca.setMinutosAntecipacao(t.minutosAntecipacao());
        presenca.setMinutosAtraso(t.minutosAtrasoEntrada());
    }

    /**
     * Troca os pares derivados de evento. Os pares MANUAIS sobrevivem por
     * padrao: quem os criou foi gente, com motivo gravado, e um job nao
     * apaga o trabalho de um humano sem ordem explicita.
     */
    private void regravarPares(UUID tenantId, UUID presencaId, List<CalculoPermanencia.Intervalo> intervalos,
                               boolean descartarManuais) {
        if (presencaId != null) {
            parRepository.deleteByTenantIdAndPresencaIdAndOrigem(tenantId, presencaId, OrigemPar.EVENTO);
            if (descartarManuais) {
                parRepository.deleteByTenantIdAndPresencaIdAndOrigem(tenantId, presencaId, OrigemPar.MANUAL);
            }
        }
        for (CalculoPermanencia.Intervalo intervalo : intervalos) {
            AccPresencaPar par = new AccPresencaPar();
            par.setTenantId(tenantId);
            par.setPresencaId(presencaId);
            par.setEntradaEm(intervalo.entrada());
            par.setSaidaEm(intervalo.saida());
            par.setMinutos(intervalo.fechado() ? intervalo.minutos() : null);
            par.setEntradaEventoId(intervalo.eventoEntradaId());
            par.setSaidaEventoId(intervalo.eventoSaidaId());
            par.setOrigem(OrigemPar.EVENTO);
            parRepository.save(par);
        }
    }

    // ---------------------------------------------------------- ajuste manual

    /**
     * Corrige, inclui ou remove um par a mao.
     *
     * Depois disso o dia vira AJUSTADA e sai do alcance do recalculo
     * automatico. A unica excecao e' a precedencia da inconsistencia: se
     * o ajuste deixou um par sem saida num dia que ja passou, o dia
     * continua INCONSISTENTE — rotular de AJUSTADA um dia que ainda esta
     * quebrado o faria entrar nos totais com numero errado.
     */
    @Transactional
    public PresencaResponse ajustarPar(AjusteParRequest request) {
        UUID tenantId = tenantObrigatorio();
        UUID operador = usuarioAtual();

        AccPresenca presenca = presencaRepository
                .findByTenantIdAndAlunoIdAndDataAndDeletedFalse(tenantId, request.alunoId(), request.data())
                .orElseGet(() -> {
                    AccPresenca nova = new AccPresenca();
                    nova.setTenantId(tenantId);
                    nova.setAlunoId(request.alunoId());
                    nova.setData(request.data());
                    nova.setStatus(StatusPresenca.AJUSTADA);
                    return nova;
                });

        if (presenca.isCongelada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dia congelado pelo fechamento não pode ser ajustado. Reabra a competência primeiro.");
        }
        if (periodoFechado(tenantId, presenca.getUnitId(), request.data())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Competência fechada. Reabra o fechamento antes de ajustar o dia.");
        }
        if (presenca.getId() == null) {
            presenca = presencaRepository.save(presenca);
        }

        if (request.remover()) {
            if (request.parId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o par a remover");
            }
            AccPresencaPar par = parObrigatorio(tenantId, request.parId(), presenca.getId());
            parRepository.delete(par);
        } else {
            if (request.entradaEm() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrada é obrigatória no ajuste");
            }
            if (request.saidaEm() != null && request.saidaEm().isBefore(request.entradaEm())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saída anterior à entrada");
            }
            AccPresencaPar par = request.parId() == null
                    ? novoParManual(tenantId, presenca.getId())
                    : parObrigatorio(tenantId, request.parId(), presenca.getId());
            par.setEntradaEm(request.entradaEm());
            par.setSaidaEm(request.saidaEm());
            par.setMinutos(request.saidaEm() == null ? null
                    : new CalculoPermanencia.Intervalo(request.entradaEm(), request.saidaEm(), null, null).minutos());
            // Par corrigido a mao deixa de ser derivado de evento: se
            // continuasse EVENTO, o proximo recalculo o apagaria.
            par.setOrigem(OrigemPar.MANUAL);
            par.setAjustadoPor(operador);
            par.setMotivoAjuste(request.motivo());
            parRepository.save(par);
        }

        presenca.setObservacao(truncar(request.motivo()));
        AccPresenca atualizada = recalcularTotaisDosPares(tenantId, presenca, true);
        log.info("Ajuste manual na presenca {} (aluno {} em {}) por {}: {}",
                atualizada.getId(), request.alunoId(), request.data(), operador, request.motivo());
        return PresencaResponse.from(atualizada, paresDe(tenantId, atualizada.getId()));
    }

    /**
     * Recalcula os totais a partir dos pares que estao gravados — e nao
     * dos eventos. E' o caminho do ajuste manual: reler os eventos aqui
     * desfaria a correcao no mesmo instante em que ela foi feita.
     */
    private AccPresenca recalcularTotaisDosPares(UUID tenantId, AccPresenca presenca, boolean ajustada) {
        List<AccPresencaPar> pares = paresDe(tenantId, presenca.getId());
        List<CalculoPermanencia.Intervalo> intervalos = pares.stream()
                .map(p -> new CalculoPermanencia.Intervalo(p.getEntradaEm(), p.getSaidaEm(),
                        p.getEntradaEventoId(), p.getSaidaEventoId()))
                .sorted(Comparator.comparing(CalculoPermanencia.Intervalo::entrada))
                .toList();

        StatusPresenca base = CalculoPermanencia.statusDe(intervalos, presenca.getData(), hoje());
        // Inconsistente vence ajustada: dia quebrado nao entra em total,
        // tenha sido mexido a mao ou nao.
        StatusPresenca status = (base == StatusPresenca.INCONSISTENTE || !ajustada) ? base : StatusPresenca.AJUSTADA;

        JornadaDoDia contrato = jornadaService.resolverDoDia(tenantId, presenca.getAlunoId(), presenca.getData());
        CalculoPermanencia.ParametrosDia parametros = new CalculoPermanencia.ParametrosDia(
                presenca.isDiaLetivo(), contrato.frequenta(), contrato.entradaPrevista(), contrato.saidaPrevista(),
                contrato.cargaMinutos(), contrato.toleranciaEntradaMin(), contrato.toleranciaSaidaMin(),
                contrato.regraExcedente());

        aplicarTotais(presenca, status,
                CalculoPermanencia.totalizar(intervalos, parametros, status, presenca.getData()));
        presenca.setJornadaId(contrato.jornadaId());
        return presencaRepository.save(presenca);
    }

    private AccPresencaPar novoParManual(UUID tenantId, UUID presencaId) {
        AccPresencaPar par = new AccPresencaPar();
        par.setTenantId(tenantId);
        par.setPresencaId(presencaId);
        return par;
    }

    private AccPresencaPar parObrigatorio(UUID tenantId, UUID parId, UUID presencaId) {
        AccPresencaPar par = parRepository.findByIdAndTenantId(parId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Par não encontrado"));
        if (!Objects.equals(par.getPresencaId(), presencaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Par não pertence ao dia informado");
        }
        return par;
    }

    // -------------------------------------------------------------- consultas

    /** Quem esta na unidade agora (status ABERTA no dia corrente). */
    public Page<PresencaResponse> quemEstaNaUnidade(UUID unitId, UUID turmaId, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        List<UUID> alunos = alunosDaTurma(tenantId, turmaId);
        boolean semFiltro = alunos == null;
        return presencaRepository
                .buscarAbertas(tenantId, hoje(), unitId, semFiltro, semFiltro ? List.of(ID_NULO) : alunos, pageable)
                .map(p -> PresencaResponse.from(p, paresDe(tenantId, p.getId())));
    }

    public ExtratoAlunoResponse extrato(UUID alunoId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        validarPeriodo(inicio, fim);
        Page<AccPresenca> pagina = presencaRepository
                .findByTenantIdAndAlunoIdAndDataBetweenAndDeletedFalse(tenantId, alunoId, inicio, fim, pageable);

        List<PresencaResponse> dias = pagina.getContent().stream()
                .map(p -> PresencaResponse.from(p, paresDe(tenantId, p.getId())))
                .toList();
        int inconsistentes = (int) pagina.getContent().stream()
                .filter(p -> p.getStatus() == StatusPresenca.INCONSISTENTE)
                .count();

        // Os totais vem da consulta agregada do periodo INTEIRO, nao da
        // pagina: somar o que esta na tela daria um numero diferente a cada
        // paginacao e ninguem confiaria em nenhum dos dois.
        List<AccPresencaRepository.TotaisAluno> totais = presencaRepository
                .totalizarPorAluno(tenantId, inicio, fim, null, false, List.of(alunoId));
        TotaisAlunoResponse resumo = totais.isEmpty()
                ? new TotaisAlunoResponse(alunoId, 0, 0, 0, 0, 0)
                : TotaisAlunoResponse.from(totais.get(0));

        return new ExtratoAlunoResponse(alunoId, inicio, fim, dias, inconsistentes, resumo);
    }

    /** Alunos com excedente no periodo, do maior para o menor. */
    public Page<TotaisAlunoResponse> excedentes(LocalDate inicio, LocalDate fim, UUID unitId, UUID turmaId,
                                                Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        validarPeriodo(inicio, fim);
        List<UUID> alunos = alunosDaTurma(tenantId, turmaId);
        boolean semFiltro = alunos == null;
        List<TotaisAlunoResponse> linhas = presencaRepository
                .totalizarExcedentes(tenantId, inicio, fim, unitId, semFiltro, semFiltro ? List.of(ID_NULO) : alunos)
                .stream()
                .map(TotaisAlunoResponse::from)
                .sorted(Comparator.comparingLong(TotaisAlunoResponse::minutosExcedente).reversed())
                .toList();
        return paginar(linhas, pageable);
    }

    public ResumoPermanenciaResponse resumo(LocalDate inicio, LocalDate fim, UUID turmaId, UUID unitId) {
        UUID tenantId = tenantObrigatorio();
        validarPeriodo(inicio, fim);
        List<UUID> alunos = alunosDaTurma(tenantId, turmaId);
        boolean semFiltro = alunos == null;
        List<TotaisAlunoResponse> porAluno = presencaRepository
                .totalizarPorAluno(tenantId, inicio, fim, unitId, semFiltro, semFiltro ? List.of(ID_NULO) : alunos)
                .stream()
                .map(TotaisAlunoResponse::from)
                .sorted(Comparator.comparingLong(TotaisAlunoResponse::minutosExcedente).reversed())
                .toList();
        return new ResumoPermanenciaResponse(inicio, fim, turmaId, unitId, porAluno,
                ResumoPermanenciaResponse.TotaisTurma.de(porAluno));
    }

    // ------------------------------------------------------------- fechamento

    /**
     * Fecha a competencia e congela todas as presencas do periodo. Os
     * dias INCONSISTENTES tambem congelam: eles nao entram no total, mas
     * tambem nao podem mudar depois que a fatura saiu — quem quiser
     * corrigi-los reabre a competencia, e isso fica registrado.
     */
    @Transactional
    public FechamentoResponse fechar(FechamentoRequest request) {
        UUID tenantId = tenantObrigatorio();
        UUID operador = usuarioAtual();
        YearMonth competencia = competenciaDe(request.competencia());
        LocalDate inicio = competencia.atDay(1);
        LocalDate fim = competencia.atEndOfMonth();

        if (!fim.isBefore(hoje())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Competência ainda não terminou: só é possível fechar mês encerrado");
        }

        AccFechamento fechamento = fechamentoRepository
                .buscarPorCompetencia(tenantId, request.unitId(), request.competencia())
                .orElseGet(() -> {
                    AccFechamento novo = new AccFechamento();
                    novo.setTenantId(tenantId);
                    novo.setUnitId(request.unitId());
                    novo.setCompetencia(request.competencia());
                    return novo;
                });
        if (fechamento.estaFechado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Competência já está fechada");
        }
        fechamento.setDataInicio(inicio);
        fechamento.setDataFim(fim);
        fechamento.setStatus(AccFechamento.FECHADO);
        fechamento.setFechadoPor(operador);
        fechamento.setFechadoEm(Instant.now(clock));
        AccFechamento salvo = fechamentoRepository.save(fechamento);

        Instant agora = Instant.now(clock);
        List<AccPresenca> presencas = presencaRepository.buscarDoPeriodo(tenantId, inicio, fim, request.unitId());
        for (AccPresenca p : presencas) {
            p.setCongelada(true);
            p.setCongeladaEm(agora);
        }
        presencaRepository.saveAll(presencas);

        log.info("Competencia {} fechada por {} ({} presencas congeladas)",
                request.competencia(), operador, presencas.size());
        return FechamentoResponse.from(salvo, presencas.size());
    }

    /**
     * Reabre a competencia e descongela o periodo.
     *
     * O acc_fechamentos de V39 nao tem coluna para quem reabriu, entao o
     * registro vai para audit_logs — sem isso, reabrir seria uma operacao
     * que muda fatura e nao deixa rastro.
     */
    @Transactional
    public FechamentoResponse reabrir(UUID fechamentoId) {
        UUID tenantId = tenantObrigatorio();
        UUID operador = usuarioAtual();
        AccFechamento fechamento = fechamentoRepository.findByIdAndTenantId(fechamentoId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fechamento não encontrado"));
        if (!fechamento.estaFechado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fechamento não está fechado");
        }

        List<AccPresenca> presencas = presencaRepository.buscarDoPeriodo(
                tenantId, fechamento.getDataInicio(), fechamento.getDataFim(), fechamento.getUnitId());
        for (AccPresenca p : presencas) {
            p.setCongelada(false);
            p.setCongeladaEm(null);
        }
        presencaRepository.saveAll(presencas);

        fechamento.setStatus(AccFechamento.REABERTO);
        AccFechamento salvo = fechamentoRepository.save(fechamento);

        AuditLog auditoria = new AuditLog();
        auditoria.setTenantId(tenantId);
        auditoria.setUserId(operador);
        auditoria.setAction("ACCESS_FECHAMENTO_REABERTO");
        auditoria.setEntity("acc_fechamentos");
        auditoria.setEntityId(fechamento.getId());
        auditoria.setTimestamp(Instant.now(clock));
        auditLogRepository.save(auditoria);

        log.warn("Competencia {} REABERTA por {} ({} presencas descongeladas)",
                fechamento.getCompetencia(), operador, presencas.size());
        return FechamentoResponse.from(salvo, presencas.size());
    }

    public Page<FechamentoResponse> listarFechamentos(Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        return fechamentoRepository.findByTenantId(tenantId, pageable).map(f -> FechamentoResponse.from(f, 0));
    }

    // -------------------------------------------------------- reprocessamento

    /** Recalculo forcado de um periodo. Dia congelado continua intocado. */
    @Transactional
    public RecalculoResponse recalcularPeriodo(RecalculoRequest request) {
        UUID tenantId = tenantObrigatorio();
        validarPeriodo(request.inicio(), request.fim());

        List<UUID> alunos = request.alunoId() != null
                ? List.of(request.alunoId())
                : alunosDoPeriodo(tenantId, request.inicio(), request.fim());

        int recalculados = 0;
        int ignorados = 0;
        for (UUID alunoId : alunos) {
            for (LocalDate dia = request.inicio(); !dia.isAfter(request.fim()); dia = dia.plusDays(1)) {
                ResultadoRecalculo r = reconstruir(tenantId, alunoId, dia, request.incluirAjustadas());
                if (r == ResultadoRecalculo.RECALCULADO) {
                    recalculados++;
                } else if (r != ResultadoRecalculo.SEM_DADOS) {
                    ignorados++;
                }
            }
        }
        log.info("Recalculo forcado {}..{} para {} alunos: {} dias refeitos, {} ignorados",
                request.inicio(), request.fim(), alunos.size(), recalculados, ignorados);
        return new RecalculoResponse(request.inicio(), request.fim(), alunos.size(), recalculados, ignorados);
    }

    /** Uniao de quem tem jornada vigente e de quem ja tem presenca no periodo. */
    private List<UUID> alunosDoPeriodo(UUID tenantId, LocalDate inicio, LocalDate fim) {
        List<UUID> alunos = new ArrayList<>(
                presencaRepository.findAlunoIdsComPresencaNoPeriodo(tenantId, inicio, fim));
        for (UUID id : jornadaAlunoIds(tenantId, inicio, fim)) {
            if (!alunos.contains(id)) {
                alunos.add(id);
            }
        }
        return alunos;
    }

    private List<UUID> jornadaAlunoIds(UUID tenantId, LocalDate inicio, LocalDate fim) {
        return jornadaService.alunosComJornadaNoPeriodo(tenantId, inicio, fim);
    }

    /**
     * Usado pelo job de 10 em 10 minutos: refaz os dias de quem passou
     * pela portaria hoje. E' rede de seguranca para o caso de o evento
     * ter sido gravado e o listener ter falhado.
     */
    @Transactional
    public int recalcularDiaCorrente(UUID tenantId, LocalDate dia) {
        int total = 0;
        for (UUID alunoId : eventoLeitor.alunosComEventoNoDia(tenantId, dia)) {
            if (reconstruir(tenantId, alunoId, dia, false) == ResultadoRecalculo.RECALCULADO) {
                total++;
            }
        }
        return total;
    }

    /**
     * Job das 03:00: o que ficou ABERTA em dia que ja passou nao e' aluno
     * na escola — e' saida que ninguem registrou. Reconstruir o dia agora
     * o classifica como INCONSISTENTE, porque {@code dia < hoje}.
     */
    @Transactional
    public int fecharDiasAnteriores(LocalDate limite) {
        List<AccPresenca> abertas = presencaRepository
                .findByStatusAndDataLessThanEqualAndDeletedFalse(StatusPresenca.ABERTA, limite);
        int total = 0;
        for (AccPresenca p : abertas) {
            if (reconstruir(p.getTenantId(), p.getAlunoId(), p.getData(), false) == ResultadoRecalculo.RECALCULADO) {
                total++;
            }
        }
        return total;
    }

    public List<UUID> tenantsComMovimento(LocalDate dia) {
        return eventoLeitor.tenantsComEventoNoDia(dia);
    }

    // ---------------------------------------------------------------- helpers

    /** Placeholder para a clausula IN quando nao ha filtro de turma. */
    private static final UUID ID_NULO = new UUID(0L, 0L);

    private List<UUID> alunosDaTurma(UUID tenantId, UUID turmaId) {
        if (turmaId == null) {
            return null;
        }
        List<UUID> ids = matriculaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, turmaId)
                .stream()
                .filter(m -> !"cancelada".equalsIgnoreCase(m.getStatus()))
                .map(m -> m.getAlunoId())
                .distinct()
                .toList();
        // Turma sem aluno nao pode virar "sem filtro": devolveria a escola
        // inteira no relatorio de uma turma vazia.
        return ids.isEmpty() ? List.of(ID_NULO) : ids;
    }

    private boolean periodoFechado(UUID tenantId, UUID unitId, LocalDate dia) {
        return fechamentoRepository.buscarFechadosQueCobrem(tenantId, dia).stream()
                .anyMatch(f -> f.getUnitId() == null || f.getUnitId().equals(unitId));
    }

    private boolean ehDiaLetivo(UUID tenantId, UUID unitId, LocalDate dia) {
        CalendarioPort calendario = calendarioProvider.getIfAvailable();
        return calendario == null || calendario.ehDiaLetivo(tenantId, unitId, dia);
    }

    private List<AccPresencaPar> paresDe(UUID tenantId, UUID presencaId) {
        if (presencaId == null) {
            return List.of();
        }
        return parRepository.findByTenantIdAndPresencaIdOrderByEntradaEmAsc(tenantId, presencaId);
    }

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(CalculoPermanencia.ZONE));
    }

    private LocalDate diaDe(Instant momento) {
        return momento.atZone(CalculoPermanencia.ZONE).toLocalDate();
    }

    private static YearMonth competenciaDe(String competencia) {
        try {
            return YearMonth.parse(competencia);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência inválida, use YYYY-MM");
        }
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe início e fim do período");
        }
        if (fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fim do período é anterior ao início");
        }
        if (inicio.plusDays(370).isBefore(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período máximo de consulta é de um ano");
        }
    }

    private static <T> Page<T> paginar(List<T> linhas, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(linhas);
        }
        int de = (int) Math.min(pageable.getOffset(), linhas.size());
        int ate = Math.min(de + pageable.getPageSize(), linhas.size());
        return new PageImpl<>(linhas.subList(de, ate), pageable, linhas.size());
    }

    private static String truncar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 255 ? texto : texto.substring(0, 255);
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }

    /** Operador da requisicao. Nulo nos jobs, que nao tem usuario. */
    private UUID usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}
