package br.com.alfaschool.backend.application.access.jornada;

import br.com.alfaschool.backend.application.access.jornada.dto.*;
import br.com.alfaschool.backend.domain.access.jornada.AccAlunoJornada;
import br.com.alfaschool.backend.domain.access.jornada.AccJornada;
import br.com.alfaschool.backend.domain.access.jornada.AccJornadaDia;
import br.com.alfaschool.backend.domain.access.jornada.AccJornadaExcecao;
import br.com.alfaschool.backend.domain.access.shared.RegraExcedente;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAlunoJornadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccJornadaDiaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccJornadaExcecaoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccJornadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatriculaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Jornadas contratadas, vinculos com vigencia e excecoes pontuais.
 *
 * A jornada e' do ALUNO, nunca da turma: a mesma turma tem o Pedro que
 * sai ao meio-dia e a Ana que fica ate as 17h. Por isso nao existe
 * "jornada da turma" aqui — existe aplicar a mesma jornada a varios
 * alunos de uma vez, que e' outra coisa.
 */
@Service
public class JornadaService {

    private static final Logger log = LoggerFactory.getLogger(JornadaService.class);

    /** "Hoje" e' o dia civil da escola, nao o do servidor. */
    private static final java.time.ZoneId ZONE_ESCOLA = java.time.ZoneId.of("America/Sao_Paulo");

    private final AccJornadaRepository jornadaRepository;
    private final AccJornadaDiaRepository jornadaDiaRepository;
    private final AccAlunoJornadaRepository alunoJornadaRepository;
    private final AccJornadaExcecaoRepository excecaoRepository;
    private final MatriculaRepository matriculaRepository;

    public JornadaService(AccJornadaRepository jornadaRepository,
                          AccJornadaDiaRepository jornadaDiaRepository,
                          AccAlunoJornadaRepository alunoJornadaRepository,
                          AccJornadaExcecaoRepository excecaoRepository,
                          MatriculaRepository matriculaRepository) {
        this.jornadaRepository = jornadaRepository;
        this.jornadaDiaRepository = jornadaDiaRepository;
        this.alunoJornadaRepository = alunoJornadaRepository;
        this.excecaoRepository = excecaoRepository;
        this.matriculaRepository = matriculaRepository;
    }

    // ---------------------------------------------------------------- jornadas

    public Page<JornadaResponse> listar(String q, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        Page<AccJornada> pagina = (q == null || q.isBlank())
                ? jornadaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable)
                : jornadaRepository.findByTenantIdAndNomeContainingIgnoreCaseAndDeletedFalse(tenantId, q, pageable);
        return pagina.map(j -> JornadaResponse.from(j, diasDe(tenantId, j.getId())));
    }

    public JornadaResponse buscar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccJornada j = jornadaObrigatoria(tenantId, id);
        return JornadaResponse.from(j, diasDe(tenantId, id));
    }

    @Transactional
    public JornadaResponse criar(JornadaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccJornada jornada = new AccJornada();
        jornada.setTenantId(tenantId);
        aplicar(jornada, request);
        AccJornada salva = jornadaRepository.save(jornada);
        substituirDias(tenantId, salva.getId(), request.dias());
        return JornadaResponse.from(salva, diasDe(tenantId, salva.getId()));
    }

    @Transactional
    public JornadaResponse atualizar(UUID id, JornadaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccJornada jornada = jornadaObrigatoria(tenantId, id);
        aplicar(jornada, request);
        AccJornada salva = jornadaRepository.save(jornada);
        // dias nulo = a chamada so' mexeu no cabecalho; substituir por lista
        // vazia apagaria a grade inteira sem ninguem ter pedido.
        if (request.dias() != null) {
            substituirDias(tenantId, id, request.dias());
        }
        return JornadaResponse.from(salva, diasDe(tenantId, id));
    }

    @Transactional
    public void remover(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccJornada jornada = jornadaObrigatoria(tenantId, id);
        // Jornada com vinculo vigente nao some: a apuracao do mes em curso
        // ainda a resolve, e sem ela o previsto de todo mundo viraria zero.
        long emUso = alunoJornadaRepository.contarVigentesDaJornada(tenantId, id, LocalDate.now(ZONE_ESCOLA));
        if (emUso > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Jornada tem vínculo vigente e não pode ser removida. Encerre os vínculos antes.");
        }
        jornada.setDeleted(true);
        jornadaRepository.save(jornada);
    }

    private void aplicar(AccJornada jornada, JornadaRequest r) {
        jornada.setNome(r.nome());
        jornada.setDescricao(r.descricao());
        jornada.setToleranciaEntradaMin(r.toleranciaEntradaMin() == null ? 0 : r.toleranciaEntradaMin());
        jornada.setToleranciaSaidaMin(r.toleranciaSaidaMin() == null ? 0 : r.toleranciaSaidaMin());
        jornada.setRegraExcedente(r.regraExcedente() == null ? RegraExcedente.HORARIO : r.regraExcedente());
        jornada.setAtivo(r.ativo() == null || r.ativo());
    }

    private void substituirDias(UUID tenantId, UUID jornadaId, List<JornadaDiaRequest> dias) {
        List<JornadaDiaRequest> entrada = dias == null ? List.of() : dias;
        Set<Integer> vistos = new HashSet<>();
        for (JornadaDiaRequest d : entrada) {
            if (!vistos.add(d.diaSemana())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dia da semana repetido na jornada: " + d.diaSemana());
            }
        }

        List<AccJornadaDia> existentes = jornadaDiaRepository
                .findByTenantIdAndJornadaIdAndDeletedFalseOrderByDiaSemanaAsc(tenantId, jornadaId);

        for (int diaSemana = 1; diaSemana <= 7; diaSemana++) {
            final int dia = diaSemana;
            AccJornadaDia alvo = existentes.stream()
                    .filter(e -> e.getDiaSemana() == dia)
                    .findFirst()
                    .orElseGet(() -> {
                        AccJornadaDia novo = new AccJornadaDia();
                        novo.setTenantId(tenantId);
                        novo.setJornadaId(jornadaId);
                        novo.setDiaSemana(dia);
                        return novo;
                    });

            JornadaDiaRequest req = entrada.stream()
                    .filter(d -> d.diaSemana() != null && d.diaSemana() == dia)
                    .findFirst()
                    .orElse(null);

            if (req == null) {
                // Dia nao informado = nao frequenta. Deixar o valor antigo
                // faria a grade nova herdar carga de um plano que acabou.
                alvo.setFrequenta(false);
                alvo.setEntradaPrevista(null);
                alvo.setSaidaPrevista(null);
                alvo.setCargaMinutos(0);
            } else {
                alvo.setFrequenta(req.frequenta());
                alvo.setEntradaPrevista(req.entradaPrevista());
                alvo.setSaidaPrevista(req.saidaPrevista());
                alvo.setCargaMinutos(cargaResolvida(req));
            }
            alvo.setDeleted(false);
            jornadaDiaRepository.save(alvo);
        }
    }

    /**
     * Carga informada vence; sem ela, deriva de entrada/saida prevista.
     * Dia que frequenta sem carga nem horario e' recusado: previsto zero
     * transformaria toda a permanencia do aluno em excedente cobravel.
     */
    private int cargaResolvida(JornadaDiaRequest req) {
        if (!req.frequenta()) {
            return 0;
        }
        if (req.cargaMinutos() != null && req.cargaMinutos() > 0) {
            return req.cargaMinutos();
        }
        Integer derivada = minutosEntre(req.entradaPrevista(), req.saidaPrevista());
        if (derivada == null || derivada <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dia " + req.diaSemana() + " frequenta mas não tem carga nem horário previsto");
        }
        return derivada;
    }

    private List<AccJornadaDia> diasDe(UUID tenantId, UUID jornadaId) {
        return jornadaDiaRepository.findByTenantIdAndJornadaIdAndDeletedFalseOrderByDiaSemanaAsc(tenantId, jornadaId);
    }

    private AccJornada jornadaObrigatoria(UUID tenantId, UUID id) {
        return jornadaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jornada não encontrada"));
    }

    // ---------------------------------------------------------------- vinculos

    public Page<AlunoJornadaResponse> listarVinculos(UUID alunoId, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        return alunoJornadaRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId, pageable)
                .map(AlunoJornadaResponse::from);
    }

    @Transactional
    public AlunoJornadaResponse vincular(AlunoJornadaRequest request) {
        UUID tenantId = tenantObrigatorio();
        jornadaObrigatoria(tenantId, request.jornadaId());
        validarVigencia(request.vigenciaInicio(), request.vigenciaFim());
        garantirSemSobreposicao(tenantId, request.alunoId(), request.vigenciaInicio(), request.vigenciaFim(), null);

        AccAlunoJornada vinculo = new AccAlunoJornada();
        vinculo.setTenantId(tenantId);
        vinculo.setAlunoId(request.alunoId());
        vinculo.setJornadaId(request.jornadaId());
        vinculo.setVigenciaInicio(request.vigenciaInicio());
        vinculo.setVigenciaFim(request.vigenciaFim());
        vinculo.setObservacao(request.observacao());
        return AlunoJornadaResponse.from(alunoJornadaRepository.save(vinculo));
    }

    @Transactional
    public AlunoJornadaResponse atualizarVinculo(UUID id, AlunoJornadaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccAlunoJornada vinculo = alunoJornadaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
        jornadaObrigatoria(tenantId, request.jornadaId());
        validarVigencia(request.vigenciaInicio(), request.vigenciaFim());
        garantirSemSobreposicao(tenantId, request.alunoId(), request.vigenciaInicio(), request.vigenciaFim(), id);

        vinculo.setAlunoId(request.alunoId());
        vinculo.setJornadaId(request.jornadaId());
        vinculo.setVigenciaInicio(request.vigenciaInicio());
        vinculo.setVigenciaFim(request.vigenciaFim());
        vinculo.setObservacao(request.observacao());
        return AlunoJornadaResponse.from(alunoJornadaRepository.save(vinculo));
    }

    @Transactional
    public void removerVinculo(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccAlunoJornada vinculo = alunoJornadaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
        vinculo.setDeleted(true);
        alunoJornadaRepository.save(vinculo);
    }

    /**
     * Aplica a mesma jornada a varios alunos. Aluno que ja tem vinculo
     * conflitante e' ignorado com motivo em vez de derrubar o lote todo.
     */
    @Transactional
    public AplicarJornadaResponse aplicarEmLote(AplicarJornadaRequest request) {
        UUID tenantId = tenantObrigatorio();
        jornadaObrigatoria(tenantId, request.jornadaId());
        validarVigencia(request.vigenciaInicio(), request.vigenciaFim());

        List<AlunoJornadaResponse> criados = new ArrayList<>();
        List<AplicarJornadaResponse.Ignorado> ignorados = new ArrayList<>();

        LocalDate vespera = request.vigenciaInicio().minusDays(1);

        for (UUID alunoId : request.alunoIds().stream().distinct().toList()) {
            try {
                List<AccAlunoJornada> existentes = alunoJornadaRepository
                        .findByTenantIdAndAlunoIdAndDeletedFalseOrderByVigenciaInicioDesc(tenantId, alunoId);
                List<AccAlunoJornada> aEncerrar = request.encerrarVinculoAnterior()
                        ? existentes.stream().filter(v -> podeSerEncerrado(v, request.vigenciaInicio())).toList()
                        : List.of();

                // A checagem vem ANTES de qualquer gravacao: se ela rodasse
                // depois do encerramento, um aluno recusado no lote ficaria
                // com o plano antigo fechado e nenhum no lugar — sem jornada
                // vigente, sem previsto, e toda a permanencia dele viraria
                // excedente cobravel no dia seguinte.
                existentes.stream()
                        .filter(v -> !aEncerrar.contains(v))
                        .filter(v -> sobrepoe(v.getVigenciaInicio(), v.getVigenciaFim(),
                                request.vigenciaInicio(), request.vigenciaFim()))
                        .findFirst()
                        .ifPresent(JornadaService::recusarPorSobreposicao);

                for (AccAlunoJornada antigo : aEncerrar) {
                    antigo.setVigenciaFim(vespera);
                    alunoJornadaRepository.save(antigo);
                }

                AccAlunoJornada vinculo = new AccAlunoJornada();
                vinculo.setTenantId(tenantId);
                vinculo.setAlunoId(alunoId);
                vinculo.setJornadaId(request.jornadaId());
                vinculo.setVigenciaInicio(request.vigenciaInicio());
                vinculo.setVigenciaFim(request.vigenciaFim());
                vinculo.setObservacao(request.observacao());
                criados.add(AlunoJornadaResponse.from(alunoJornadaRepository.save(vinculo)));
            } catch (ResponseStatusException e) {
                ignorados.add(new AplicarJornadaResponse.Ignorado(alunoId, e.getReason()));
            }
        }
        log.info("Jornada {} aplicada a {} alunos ({} ignorados)", request.jornadaId(), criados.size(), ignorados.size());
        return new AplicarJornadaResponse(request.jornadaId(), criados.size(), criados, ignorados);
    }

    /** Ids de alunos com matricula ativa na turma — atalho para o lote. */
    public List<UUID> alunosDaTurma(UUID turmaId) {
        UUID tenantId = tenantObrigatorio();
        return matriculaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, turmaId)
                .stream()
                .filter(m -> !"cancelada".equalsIgnoreCase(m.getStatus()))
                .map(m -> m.getAlunoId())
                .distinct()
                .toList();
    }

    /**
     * Vinculo que pode ser fechado na vespera do novo inicio. NAO apaga
     * nada: a apuracao de marco precisa continuar encontrando a jornada de
     * marco depois que a familia migrou de plano em agosto.
     *
     * Vinculo que comeca DEPOIS do novo inicio fica de fora — encerra-lo
     * na vespera inventaria uma vigencia negativa.
     */
    private static boolean podeSerEncerrado(AccAlunoJornada v, LocalDate novoInicio) {
        boolean abertoNoFuturo = v.getVigenciaFim() == null || !v.getVigenciaFim().isBefore(novoInicio);
        return abertoNoFuturo && !v.getVigenciaInicio().isAfter(novoInicio.minusDays(1));
    }

    /** Fim nulo vale como infinito nos dois lados. */
    private static boolean sobrepoe(LocalDate aInicio, LocalDate aFim, LocalDate bInicio, LocalDate bFim) {
        boolean comecaAntesDoFimDoOutro = bFim == null || !aInicio.isAfter(bFim);
        boolean terminaDepoisDoInicioDoOutro = aFim == null || !aFim.isBefore(bInicio);
        return comecaAntesDoFimDoOutro && terminaDepoisDoInicioDoOutro;
    }

    private void garantirSemSobreposicao(UUID tenantId, UUID alunoId, LocalDate inicio, LocalDate fim, UUID ignorarId) {
        alunoJornadaRepository.findSobrepostos(tenantId, alunoId, inicio, fim, ignorarId)
                .stream().findFirst()
                .ifPresent(JornadaService::recusarPorSobreposicao);
    }

    private static void recusarPorSobreposicao(AccAlunoJornada c) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Aluno já tem jornada vigente no período (" + c.getVigenciaInicio()
                        + " a " + (c.getVigenciaFim() == null ? "indeterminado" : c.getVigenciaFim()) + ")");
    }

    private void validarVigencia(LocalDate inicio, LocalDate fim) {
        if (fim != null && fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fim da vigência é anterior ao início");
        }
    }

    // --------------------------------------------------------------- excecoes

    @Transactional
    public JornadaExcecaoResponse salvarExcecao(JornadaExcecaoRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccJornadaExcecao excecao = excecaoRepository
                .findByTenantIdAndAlunoIdAndDataAndDeletedFalse(tenantId, request.alunoId(), request.data())
                .orElseGet(() -> {
                    AccJornadaExcecao nova = new AccJornadaExcecao();
                    nova.setTenantId(tenantId);
                    nova.setAlunoId(request.alunoId());
                    nova.setData(request.data());
                    return nova;
                });
        excecao.setFrequenta(request.frequenta());
        excecao.setEntradaPrevista(request.entradaPrevista());
        excecao.setSaidaPrevista(request.saidaPrevista());
        excecao.setCargaMinutos(request.cargaMinutos());
        excecao.setMotivo(request.motivo());
        return JornadaExcecaoResponse.from(excecaoRepository.save(excecao));
    }

    public List<JornadaExcecaoResponse> listarExcecoes(UUID alunoId, LocalDate inicio, LocalDate fim) {
        UUID tenantId = tenantObrigatorio();
        return excecaoRepository
                .findByTenantIdAndAlunoIdAndDataBetweenAndDeletedFalse(tenantId, alunoId, inicio, fim)
                .stream().map(JornadaExcecaoResponse::from).toList();
    }

    @Transactional
    public void removerExcecao(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccJornadaExcecao excecao = excecaoRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exceção não encontrada"));
        excecao.setDeleted(true);
        excecaoRepository.save(excecao);
    }

    // ------------------------------------------------------ resolucao vigente

    /**
     * Qual jornada valia para o aluno naquela data.
     *
     * A resolucao e' sempre PELA DATA APURADA, nunca pela data de hoje:
     * e' isso que impede que trocar o plano no meio do ano reescreva o
     * passado ja faturado.
     */
    public Optional<AccAlunoJornada> vinculoVigente(UUID tenantId, UUID alunoId, LocalDate data) {
        List<AccAlunoJornada> vigentes = alunoJornadaRepository.findVigentesEm(tenantId, alunoId, data);
        return vigentes.isEmpty() ? Optional.empty() : Optional.of(vigentes.get(0));
    }

    /**
     * Alunos com jornada cruzando o periodo. Usado pelo reprocessamento:
     * quem tem plano contratado precisa ser apurado ainda que nao tenha
     * batido o cartao nenhum dia — a falta tambem e' informacao.
     */
    public List<UUID> alunosComJornadaNoPeriodo(UUID tenantId, LocalDate inicio, LocalDate fim) {
        return alunoJornadaRepository.findAlunoIdsComJornadaNoPeriodo(tenantId, inicio, fim);
    }

    /** Versao para o controller, usando o tenant do contexto. */
    public Optional<AlunoJornadaResponse> jornadaVigenteDoAluno(UUID alunoId, LocalDate data) {
        return vinculoVigente(tenantObrigatorio(), alunoId, data).map(AlunoJornadaResponse::from);
    }

    /**
     * O contratado do aluno naquele dia, com a excecao pontual aplicada
     * por cima da grade da jornada vigente.
     *
     * Precedencia: excecao do dia &gt; dia da jornada vigente &gt; sem
     * contrato. Tolerancia e regra de excedente vem SEMPRE da jornada —
     * a excecao muda o horario do dia, nao o contrato comercial.
     */
    public JornadaDoDia resolverDoDia(UUID tenantId, UUID alunoId, LocalDate data) {
        Optional<AccAlunoJornada> vinculo = vinculoVigente(tenantId, alunoId, data);
        AccJornada jornada = vinculo
                .flatMap(v -> jornadaRepository.findByIdAndTenantIdAndDeletedFalse(v.getJornadaId(), tenantId))
                .orElse(null);
        AccJornadaDia diaJornada = jornada == null ? null : jornadaDiaRepository
                .findByTenantIdAndJornadaIdAndDiaSemanaAndDeletedFalse(tenantId, jornada.getId(),
                        data.getDayOfWeek().getValue())
                .orElse(null);
        AccJornadaExcecao excecao = excecaoRepository
                .findByTenantIdAndAlunoIdAndDataAndDeletedFalse(tenantId, alunoId, data)
                .orElse(null);

        if (jornada == null && excecao == null) {
            return JornadaDoDia.semContrato();
        }

        int tolEntrada = jornada == null ? 0 : jornada.getToleranciaEntradaMin();
        int tolSaida = jornada == null ? 0 : jornada.getToleranciaSaidaMin();
        RegraExcedente regra = jornada == null ? RegraExcedente.HORARIO : jornada.getRegraExcedente();
        UUID jornadaId = jornada == null ? null : jornada.getId();

        if (excecao != null) {
            LocalTime entrada = excecao.getEntradaPrevista() != null
                    ? excecao.getEntradaPrevista()
                    : (diaJornada == null ? null : diaJornada.getEntradaPrevista());
            LocalTime saida = excecao.getSaidaPrevista() != null
                    ? excecao.getSaidaPrevista()
                    : (diaJornada == null ? null : diaJornada.getSaidaPrevista());
            int carga = 0;
            if (excecao.isFrequenta()) {
                if (excecao.getCargaMinutos() != null) {
                    carga = excecao.getCargaMinutos();
                } else {
                    Integer derivada = minutosEntre(entrada, saida);
                    // Sem carga e sem horario na excecao, o contrato do dia
                    // continua valendo: a secretaria mudou so' o motivo.
                    carga = derivada != null ? derivada : (diaJornada == null ? 0 : diaJornada.getCargaMinutos());
                }
            }
            return new JornadaDoDia(jornadaId, excecao.isFrequenta(), entrada, saida, Math.max(0, carga),
                    tolEntrada, tolSaida, regra, true);
        }

        if (diaJornada == null || !diaJornada.isFrequenta()) {
            return new JornadaDoDia(jornadaId, false, null, null, 0, tolEntrada, tolSaida, regra, false);
        }
        return new JornadaDoDia(jornadaId, true, diaJornada.getEntradaPrevista(), diaJornada.getSaidaPrevista(),
                Math.max(0, diaJornada.getCargaMinutos()), tolEntrada, tolSaida, regra, false);
    }

    // ---------------------------------------------------------------- helpers

    private static Integer minutosEntre(LocalTime entrada, LocalTime saida) {
        if (entrada == null || saida == null) {
            return null;
        }
        long minutos = Duration.between(entrada, saida).toMinutes();
        return minutos <= 0 ? null : (int) minutos;
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }
}
