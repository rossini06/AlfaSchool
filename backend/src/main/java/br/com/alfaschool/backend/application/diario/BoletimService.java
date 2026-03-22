package br.com.alfaschool.backend.application.diario;

import br.com.alfaschool.backend.application.diario.dto.BoletimResponse;
import br.com.alfaschool.backend.application.diario.dto.BoletimResponse.AvaliacaoBoletim;
import br.com.alfaschool.backend.application.diario.dto.BoletimResponse.DisciplinaBoletim;
import br.com.alfaschool.backend.domain.aluno.Aluno;
import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
import br.com.alfaschool.backend.domain.curso.Curso;
import br.com.alfaschool.backend.domain.diario.Media;
import br.com.alfaschool.backend.domain.diario.SituacaoAluno;
import br.com.alfaschool.backend.domain.disciplina.Disciplina;
import br.com.alfaschool.backend.domain.matricula.Matricula;
import br.com.alfaschool.backend.domain.nota.Nota;
import br.com.alfaschool.backend.domain.turma.Turma;
import br.com.alfaschool.backend.infrastructure.persistence.repository.*;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service para geração do boletim do aluno.
 */
@Service
public class BoletimService {

    private static final Logger log = LoggerFactory.getLogger(BoletimService.class);

    private final MatriculaRepository matriculaRepository;
    private final TurmaRepository turmaRepository;
    private final CursoRepository cursoRepository;
    private final AlunoRepository alunoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final MediaRepository mediaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final NotaRepository notaRepository;
    private final MediaService mediaService;

    public BoletimService(
            MatriculaRepository matriculaRepository,
            TurmaRepository turmaRepository,
            CursoRepository cursoRepository,
            AlunoRepository alunoRepository,
            DisciplinaRepository disciplinaRepository,
            MediaRepository mediaRepository,
            AvaliacaoRepository avaliacaoRepository,
            NotaRepository notaRepository,
            MediaService mediaService) {
        this.matriculaRepository = matriculaRepository;
        this.turmaRepository = turmaRepository;
        this.cursoRepository = cursoRepository;
        this.alunoRepository = alunoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.mediaRepository = mediaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.notaRepository = notaRepository;
        this.mediaService = mediaService;
    }

    /**
     * Gera o boletim completo de uma matrícula.
     */
    public BoletimResponse gerarBoletim(UUID matriculaId, String periodo) {
        UUID tenantId = requiredTenant();

        // Buscar matrícula
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));

        // Buscar aluno
        Aluno aluno = alunoRepository.findById(matricula.getAlunoId())
                .filter(a -> !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));

        // Buscar turma
        Turma turma = turmaRepository.findById(matricula.getTurmaId())
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        // Buscar curso
        Curso curso = cursoRepository.findById(turma.getCursoId())
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso não encontrado"));

        // Buscar disciplinas do curso
        List<Disciplina> disciplinas = disciplinaRepository
                .findByTenantIdAndCursoIdAndAtivaAndDeletedFalse(tenantId, curso.getId(), true);

        // Buscar médias existentes ou calcular
        List<DisciplinaBoletim> disciplinasBoletim = new ArrayList<>();
        BigDecimal somaMedias = BigDecimal.ZERO;
        BigDecimal somaFrequencias = BigDecimal.ZERO;
        int disciplinasComMedia = 0;
        int disciplinasComFrequencia = 0;
        int totalAprovados = 0;
        int totalReprovados = 0;

        for (Disciplina disc : disciplinas) {
            // Buscar ou calcular média
            Media media = mediaRepository
                    .findByTenantIdAndMatriculaIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
                            tenantId, matriculaId, disc.getId(), periodo)
                    .orElse(null);

            // Se não existe, tentar calcular
            if (media == null) {
                try {
                    mediaService.calcularMedia(matriculaId, disc.getId(), periodo);
                    media = mediaRepository
                            .findByTenantIdAndMatriculaIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
                                    tenantId, matriculaId, disc.getId(), periodo)
                            .orElse(null);
                } catch (Exception e) {
                    log.warn("Erro ao calcular média para boletim: matriculaId={}, disciplinaId={}, periodo={}",
                            matriculaId, disc.getId(), periodo, e);
                }
            }

            // Buscar avaliações e notas
            List<AvaliacaoBoletim> avaliacoesBoletim = new ArrayList<>();
            List<Avaliacao> avaliacoes = avaliacaoRepository
                    .findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(tenantId, turma.getId(), disc.getId())
                    .stream()
                    .filter(a -> periodo == null || periodo.equals(a.getPeriodo()))
                    .toList();

            List<UUID> avaliacaoIds = avaliacoes.stream().map(Avaliacao::getId).toList();
            List<Nota> notas = avaliacaoIds.isEmpty()
                    ? List.of()
                    : notaRepository.findByMatriculaAndAvaliacoes(tenantId, matriculaId, avaliacaoIds);

            for (Avaliacao av : avaliacoes) {
                Nota nota = notas.stream()
                        .filter(n -> n.getAvaliacaoId().equals(av.getId()))
                        .findFirst()
                        .orElse(null);

                avaliacoesBoletim.add(new AvaliacaoBoletim(
                        av.getId(),
                        av.getNome(),
                        av.getTipo(),
                        av.getPeso(),
                        av.getNotaMaxima(),
                        nota != null ? nota.getNota() : null,
                        nota != null ? nota.getNotaRecuperacao() : null,
                        nota != null ? nota.getNotaFinal() : null
                ));
            }

            DisciplinaBoletim discBoletim = new DisciplinaBoletim(
                    disc.getId(),
                    disc.getNome(),
                    disc.getCodigo(),
                    disc.getCargaHoraria(),
                    media != null ? media.getMedia() : null,
                    media != null ? media.getPercentualFrequencia() : null,
                    media != null ? media.getTotalAulas() : 0,
                    media != null ? media.getTotalPresencas() : 0,
                    media != null ? media.getTotalFaltas() : 0,
                    media != null ? media.getTotalJustificadas() : 0,
                    media != null ? media.getSituacao() : SituacaoAluno.CURSANDO,
                    media != null ? media.getConceito() : null,
                    media != null ? media.getObservacaoDescritiva() : null,
                    avaliacoesBoletim
            );

            disciplinasBoletim.add(discBoletim);

            // Acumular para média geral
            if (media != null && media.getMedia() != null) {
                somaMedias = somaMedias.add(media.getMedia());
                disciplinasComMedia++;
            }
            if (media != null && media.getPercentualFrequencia() != null) {
                somaFrequencias = somaFrequencias.add(media.getPercentualFrequencia());
                disciplinasComFrequencia++;
            }
            if (media != null) {
                if (media.getSituacao() == SituacaoAluno.APROVADO) totalAprovados++;
                if (media.getSituacao() == SituacaoAluno.REPROVADO ||
                    media.getSituacao() == SituacaoAluno.REPROVADO_FREQUENCIA ||
                    media.getSituacao() == SituacaoAluno.REPROVADO_NOTA) totalReprovados++;
            }
        }

        // Calcular média geral
        BigDecimal mediaGeral = disciplinasComMedia > 0
                ? somaMedias.divide(BigDecimal.valueOf(disciplinasComMedia), 2, RoundingMode.HALF_UP)
                : null;

        BigDecimal frequenciaGeral = disciplinasComFrequencia > 0
                ? somaFrequencias.divide(BigDecimal.valueOf(disciplinasComFrequencia), 2, RoundingMode.HALF_UP)
                : null;

        // Determinar situação geral
        SituacaoAluno situacaoGeral = determinarSituacaoGeral(totalAprovados, totalReprovados, disciplinas.size());

        return new BoletimResponse(
                aluno.getId(),
                aluno.getNome(),
                matricula.getNumeroMatricula(),
                turma.getId(),
                turma.getNome(),
                turma.getAnoLetivo(),
                turma.getTurno(),
                curso.getId(),
                curso.getNome(),
                curso.getNivel(),
                periodo,
                disciplinas.size(),
                mediaGeral,
                frequenciaGeral,
                situacaoGeral,
                disciplinasBoletim
        );
    }

    private SituacaoAluno determinarSituacaoGeral(int aprovados, int reprovados, int total) {
        if (total == 0) return SituacaoAluno.CURSANDO;
        if (reprovados > 0) return SituacaoAluno.REPROVADO;
        if (aprovados == total) return SituacaoAluno.APROVADO;
        return SituacaoAluno.CURSANDO;
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        return tenantId;
    }
}
