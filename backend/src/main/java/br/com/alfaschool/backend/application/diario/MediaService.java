package br.com.alfaschool.backend.application.diario;

import br.com.alfaschool.backend.application.diario.dto.MediaResponse;
import br.com.alfaschool.backend.application.diario.strategy.EducationRuleFactory;
import br.com.alfaschool.backend.application.diario.strategy.EducationRuleStrategy;
import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
import br.com.alfaschool.backend.domain.curso.Curso;
import br.com.alfaschool.backend.domain.diario.*;
import br.com.alfaschool.backend.domain.matricula.Matricula;
import br.com.alfaschool.backend.domain.nota.Nota;
import br.com.alfaschool.backend.domain.turma.Turma;
import br.com.alfaschool.backend.infrastructure.persistence.repository.*;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service CENTRAL para cálculo de médias e situação do aluno.
 * Toda lógica de cálculo deve estar centralizada aqui.
 */
@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final NotaRepository notaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final FrequenciaRepository frequenciaRepository;
    private final MatriculaRepository matriculaRepository;
    private final TurmaRepository turmaRepository;
    private final CursoRepository cursoRepository;
    private final RegraAprovacaoRepository regraAprovacaoRepository;
    private final EducationRuleFactory educationRuleFactory;

    public MediaService(
            MediaRepository mediaRepository,
            NotaRepository notaRepository,
            AvaliacaoRepository avaliacaoRepository,
            FrequenciaRepository frequenciaRepository,
            MatriculaRepository matriculaRepository,
            TurmaRepository turmaRepository,
            CursoRepository cursoRepository,
            RegraAprovacaoRepository regraAprovacaoRepository,
            EducationRuleFactory educationRuleFactory) {
        this.mediaRepository = mediaRepository;
        this.notaRepository = notaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.frequenciaRepository = frequenciaRepository;
        this.matriculaRepository = matriculaRepository;
        this.turmaRepository = turmaRepository;
        this.cursoRepository = cursoRepository;
        this.regraAprovacaoRepository = regraAprovacaoRepository;
        this.educationRuleFactory = educationRuleFactory;
    }

    /**
     * Calcula a média do aluno em uma disciplina específica.
     * Este é o método CENTRAL para cálculo de média.
     */
    @Transactional
    public MediaResponse calcularMedia(UUID matriculaId, UUID disciplinaId, String periodo) {
        UUID tenantId = requiredTenant();

        // Buscar matrícula
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));

        // Buscar turma e curso para obter as regras
        Turma turma = turmaRepository.findById(matricula.getTurmaId())
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        Curso curso = cursoRepository.findById(turma.getCursoId())
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso não encontrado"));

        // Obter regras de aprovação
        RegraAprovacao regra = obterRegraAprovacao(tenantId, curso.getNivel());
        EducationRuleStrategy strategy = educationRuleFactory.getStrategyByNivel(curso.getNivel());

        // Buscar ou criar registro de média
        Media media = mediaRepository
                .findByTenantIdAndMatriculaIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
                        tenantId, matriculaId, disciplinaId, periodo)
                .orElseGet(() -> {
                    Media m = new Media();
                    m.setTenantId(tenantId);
                    m.setMatriculaId(matriculaId);
                    m.setDisciplinaId(disciplinaId);
                    m.setTurmaId(matricula.getTurmaId());
                    m.setPeriodo(periodo);
                    return m;
                });

        // Calcular frequência
        calcularFrequencia(media, tenantId, matriculaId, disciplinaId);

        // Coleta as notas com peso uma vez. A media NUMERICA e' a do nivel
        // (infantil devolve null de proposito); a `base` e' uma media ponderada
        // auxiliar para derivar conceito e situacao mesmo no infantil — sem ela
        // o boletim do infantil ficava totalmente vazio (media e conceito nulos).
        List<EducationRuleStrategy.NotaComPeso> notasComPeso =
                coletarNotasComPeso(tenantId, matriculaId, disciplinaId, turma.getId(), periodo);
        BigDecimal mediaNotas = notasComPeso.isEmpty() ? null : strategy.calcularMedia(notasComPeso);
        BigDecimal base = mediaNotas != null
                ? mediaNotas
                : (notasComPeso.isEmpty() ? null : mediaBase(notasComPeso));

        if (strategy.usaNotaNumerica(regra)) {
            media.setMedia(mediaNotas);
        }

        // Determinar situação
        media.setSituacao(strategy.determinarSituacao(
                base,
                media.getPercentualFrequencia(),
                regra));

        // Converter para conceito se necessário
        if (!strategy.usaNotaNumerica(regra) || (regra.getUsaConceito() != null && regra.getUsaConceito())) {
            media.setConceito(strategy.converterParaConceito(base, regra));
        }

        return MediaResponse.from(mediaRepository.save(media));
    }

    /**
     * Recalcula todas as médias de uma matrícula.
     */
    @Transactional
    public List<MediaResponse> recalcularTodasMedias(UUID matriculaId) {
        UUID tenantId = requiredTenant();

        // Buscar todas as médias existentes da matrícula
        List<Media> medias = mediaRepository.findByTenantIdAndMatriculaIdAndDeletedFalse(tenantId, matriculaId);

        List<MediaResponse> resultado = new ArrayList<>();
        for (Media m : medias) {
            resultado.add(calcularMedia(matriculaId, m.getDisciplinaId(), m.getPeriodo()));
        }

        return resultado;
    }

    /**
     * Recalcula a média quando uma nota é alterada.
     * Este método deve ser chamado pelo NotaService ao salvar/atualizar uma nota.
     */
    @Transactional
    public void recalcularAposAlteracaoNota(UUID matriculaId, UUID avaliacaoId) {
        UUID tenantId = requiredTenant();

        // Buscar avaliação para obter disciplinaId e período
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElse(null);

        if (avaliacao == null) return;

        // Recalcular média
        calcularMedia(matriculaId, avaliacao.getDisciplinaId(), avaliacao.getPeriodo());
    }

    /**
     * Lista médias de uma turma.
     */
    public List<MediaResponse> listByTurma(UUID turmaId, UUID disciplinaId) {
        UUID tenantId = requiredTenant();
        return mediaRepository
                .findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(tenantId, turmaId, disciplinaId)
                .stream()
                .map(MediaResponse::from)
                .toList();
    }

    /**
     * Lista médias de uma matrícula.
     */
    public List<MediaResponse> listByMatricula(UUID matriculaId) {
        UUID tenantId = requiredTenant();
        return mediaRepository
                .findByTenantIdAndMatriculaIdAndDeletedFalse(tenantId, matriculaId)
                .stream()
                .map(MediaResponse::from)
                .toList();
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private void calcularFrequencia(Media media, UUID tenantId, UUID matriculaId, UUID disciplinaId) {
        // Contar total de frequências
        long total = frequenciaRepository.countByMatriculaAndDisciplina(tenantId, matriculaId, disciplinaId);

        if (total == 0) {
            media.setTotalAulas(0);
            media.setTotalPresencas(0);
            media.setTotalFaltas(0);
            media.setTotalJustificadas(0);
            media.setPercentualFrequencia(null);
            return;
        }

        // Contar presenças (PRESENTE + ATRASADO)
        long presentes = frequenciaRepository.countByMatriculaAndDisciplinaAndStatus(
                tenantId, matriculaId, disciplinaId, StatusFrequencia.PRESENTE);
        long atrasados = frequenciaRepository.countByMatriculaAndDisciplinaAndStatus(
                tenantId, matriculaId, disciplinaId, StatusFrequencia.ATRASADO);

        // Contar ausências
        long ausentes = frequenciaRepository.countByMatriculaAndDisciplinaAndStatus(
                tenantId, matriculaId, disciplinaId, StatusFrequencia.AUSENTE);

        // Contar justificadas
        long justificadas = frequenciaRepository.countByMatriculaAndDisciplinaAndStatus(
                tenantId, matriculaId, disciplinaId, StatusFrequencia.JUSTIFICADO);

        long totalPresencas = presentes + atrasados + justificadas;
        long totalFaltas = ausentes;

        media.setTotalAulas((int) total);
        media.setTotalPresencas((int) totalPresencas);
        media.setTotalFaltas((int) totalFaltas);
        media.setTotalJustificadas((int) justificadas);

        // Calcular percentual: (presenças / total) * 100
        BigDecimal percentual = BigDecimal.valueOf(totalPresencas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        media.setPercentualFrequencia(percentual);
    }

    /** Junta as notas do aluno com o peso/nota-maxima de cada avaliacao da
     *  disciplina/turma/periodo. Lista vazia = sem avaliacao ou sem nota. */
    private List<EducationRuleStrategy.NotaComPeso> coletarNotasComPeso(
            UUID tenantId, UUID matriculaId, UUID disciplinaId, UUID turmaId, String periodo) {

        List<Avaliacao> avaliacoes = avaliacaoRepository
                .findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(tenantId, turmaId, disciplinaId)
                .stream()
                .filter(a -> periodo == null || periodo.equals(a.getPeriodo()))
                .toList();
        if (avaliacoes.isEmpty()) {
            return List.of();
        }

        List<UUID> avaliacaoIds = avaliacoes.stream().map(Avaliacao::getId).toList();
        List<Nota> notas = notaRepository.findByMatriculaAndAvaliacoes(tenantId, matriculaId, avaliacaoIds);

        List<EducationRuleStrategy.NotaComPeso> notasComPeso = new ArrayList<>();
        for (Nota nota : notas) {
            Avaliacao av = avaliacoes.stream()
                    .filter(a -> a.getId().equals(nota.getAvaliacaoId()))
                    .findFirst()
                    .orElse(null);
            if (av != null && nota.getNotaFinal() != null) {
                notasComPeso.add(new EducationRuleStrategy.NotaComPeso(
                        nota, av.getPeso(), av.getNotaMaxima()));
            }
        }
        return notasComPeso;
    }

    /** Media ponderada normalizada para 0-10. Base para derivar CONCEITO e
     *  situacao quando o nivel nao usa nota numerica (ex.: infantil). */
    private BigDecimal mediaBase(List<EducationRuleStrategy.NotaComPeso> notas) {
        BigDecimal somaPonderada = BigDecimal.ZERO;
        BigDecimal somaPesos = BigDecimal.ZERO;
        for (EducationRuleStrategy.NotaComPeso item : notas) {
            if (item == null || item.nota() == null || item.nota().getNotaFinal() == null || item.peso() == null) {
                continue;
            }
            BigDecimal nota = item.nota().getNotaFinal();
            if (item.notaMaxima() != null
                    && item.notaMaxima().compareTo(BigDecimal.ZERO) > 0
                    && item.notaMaxima().compareTo(BigDecimal.TEN) != 0) {
                nota = nota.multiply(BigDecimal.TEN).divide(item.notaMaxima(), 2, RoundingMode.HALF_UP);
            }
            somaPonderada = somaPonderada.add(nota.multiply(item.peso()));
            somaPesos = somaPesos.add(item.peso());
        }
        if (somaPesos.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return somaPonderada.divide(somaPesos, 2, RoundingMode.HALF_UP);
    }

    private RegraAprovacao obterRegraAprovacao(UUID tenantId, String nivel) {
        // Tentar encontrar regra específica do tenant
        TipoEnsino tipo = inferirTipoEnsino(nivel);

        return regraAprovacaoRepository
                .findByTenantIdAndTipoEnsinoAndDeletedFalse(tenantId, tipo)
                .orElseGet(() -> {
                    // Usar regra padrão
                    UUID defaultTenantId = UUID.fromString("00000000-0000-0000-0000-000000000000");
                    return regraAprovacaoRepository
                            .findByTenantIdAndTipoEnsinoAndDeletedFalse(defaultTenantId, tipo)
                            .orElseGet(() -> criarRegraDefault(tipo));
                });
    }

    private TipoEnsino inferirTipoEnsino(String nivel) {
        if (nivel == null) return TipoEnsino.FUNDAMENTAL;

        String nivelLower = nivel.toLowerCase();
        if (nivelLower.contains("infantil")) return TipoEnsino.INFANTIL;
        if (nivelLower.contains("médio") || nivelLower.contains("medio")) return TipoEnsino.MEDIO;
        if (nivelLower.contains("técnico") || nivelLower.contains("tecnico")) return TipoEnsino.TECNICO;
        return TipoEnsino.FUNDAMENTAL;
    }

    private RegraAprovacao criarRegraDefault(TipoEnsino tipo) {
        RegraAprovacao regra = new RegraAprovacao();
        regra.setTipoEnsino(tipo);
        regra.setNotaMinimaAprovacao(new BigDecimal("6.00"));
        regra.setFrequenciaMinimaAprovacao(new BigDecimal("75.00"));
        regra.setPermiteRecuperacao(true);

        if (tipo == TipoEnsino.INFANTIL) {
            regra.setUsaNotaNumerica(false);
            regra.setUsaConceito(true);
            regra.setConceitosPossiveis("Ótimo,Bom,Regular");
            regra.setUsaAvaliacaoDescritiva(true);
            regra.setPermiteRecuperacao(false);
        }

        return regra;
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        return tenantId;
    }
}
