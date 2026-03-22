package br.com.alfaschool.backend.application.nota;

import br.com.alfaschool.backend.application.diario.MediaService;
import br.com.alfaschool.backend.application.nota.dto.NotaRequest;
import br.com.alfaschool.backend.application.nota.dto.NotaResponse;
import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
import br.com.alfaschool.backend.domain.diario.HistoricoNota;
import br.com.alfaschool.backend.domain.nota.Nota;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AvaliacaoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.HistoricoNotaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.NotaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotaService {

    private static final Logger log = LoggerFactory.getLogger(NotaService.class);

    private final NotaRepository notaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final HistoricoNotaRepository historicoNotaRepository;
    private final MediaService mediaService;

    public NotaService(
            NotaRepository notaRepository,
            AvaliacaoRepository avaliacaoRepository,
            HistoricoNotaRepository historicoNotaRepository,
            @Lazy MediaService mediaService) {
        this.notaRepository = notaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.historicoNotaRepository = historicoNotaRepository;
        this.mediaService = mediaService;
    }

    public List<NotaResponse> listByAluno(UUID alunoId) {
        UUID tenantId = requiredTenant();
        return notaRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream().map(NotaResponse::from).toList();
    }

    public List<NotaResponse> listByMatricula(UUID matriculaId) {
        UUID tenantId = requiredTenant();
        return notaRepository.findByTenantIdAndMatriculaIdAndDeletedFalse(tenantId, matriculaId)
                .stream().map(NotaResponse::from).toList();
    }

    public List<NotaResponse> listByAvaliacao(UUID avaliacaoId) {
        UUID tenantId = requiredTenant();
        return notaRepository.findByTenantIdAndAvaliacaoIdAndDeletedFalse(tenantId, avaliacaoId)
                .stream().map(NotaResponse::from).toList();
    }

    @Transactional
    public NotaResponse lancar(NotaRequest request) {
        UUID tenantId = requiredTenant();

        // Buscar avaliação
        Avaliacao avaliacao = avaliacaoRepository
                .findByIdAndTenantIdAndDeletedFalse(request.avaliacaoId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        BigDecimal notaMaxima = avaliacao.getNotaMaxima();

        // Validar nota
        if (request.nota() != null) {
            if (request.nota().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nota não pode ser negativa");
            }
            if (notaMaxima != null && request.nota().compareTo(notaMaxima) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nota " + request.nota() + " excede a nota máxima da avaliação (" + notaMaxima + ")");
            }
        }

        // Validar nota de recuperação
        if (request.notaRecuperacao() != null) {
            // Verificar se a avaliação permite recuperação
            if (avaliacao.getPermiteRecuperacao() != null && !avaliacao.getPermiteRecuperacao()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Esta avaliação não permite recuperação");
            }
            if (request.notaRecuperacao().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nota de recuperação não pode ser negativa");
            }
            if (notaMaxima != null && request.notaRecuperacao().compareTo(notaMaxima) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nota de recuperação excede a nota máxima da avaliação (" + notaMaxima + ")");
            }
        }

        // Upsert: atualiza se já existe, cria se não existe
        Nota nota = notaRepository.findByTenantIdAndAlunoIdAndAvaliacaoIdAndDeletedFalse(
                        tenantId, request.alunoId(), request.avaliacaoId())
                .orElse(null);

        boolean isNew = nota == null;
        BigDecimal notaAnterior = null;

        if (isNew) {
            nota = new Nota();
            nota.setTenantId(tenantId);
            nota.setAlunoId(request.alunoId());
            nota.setAvaliacaoId(request.avaliacaoId());
        } else {
            notaAnterior = nota.getNota();
        }

        nota.setMatriculaId(request.matriculaId());
        nota.setNota(request.nota());
        nota.setNotaRecuperacao(request.notaRecuperacao());
        nota.setObs(request.obs());

        // Calcular nota final automaticamente
        nota.calcularNotaFinal();

        Nota saved = notaRepository.save(nota);

        // Registrar histórico se houve alteração
        if (!isNew && notaAnterior != null && !notaAnterior.equals(request.nota())) {
            registrarHistorico(saved, notaAnterior, request.nota(), "ALTERACAO");
        }

        // Recalcular média automaticamente
        if (request.matriculaId() != null) {
            try {
                mediaService.recalcularAposAlteracaoNota(request.matriculaId(), request.avaliacaoId());
            } catch (Exception e) {
                log.warn("Erro ao recalcular média após lançamento de nota: matriculaId={}, avaliacaoId={}",
                        request.matriculaId(), request.avaliacaoId(), e);
            }
        }

        return NotaResponse.from(saved);
    }

    /**
     * Lançar nota de recuperação separadamente.
     */
    @Transactional
    public NotaResponse lancarRecuperacao(UUID notaId, BigDecimal notaRecuperacao) {
        UUID tenantId = requiredTenant();

        Nota nota = notaRepository.findById(notaId)
                .filter(n -> tenantId.equals(n.getTenantId()) && !Boolean.TRUE.equals(n.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada"));

        // Buscar avaliação para validar
        Avaliacao avaliacao = avaliacaoRepository
                .findByIdAndTenantIdAndDeletedFalse(nota.getAvaliacaoId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        // Verificar se permite recuperação
        if (avaliacao.getPermiteRecuperacao() != null && !avaliacao.getPermiteRecuperacao()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Esta avaliação não permite recuperação");
        }

        // Validar nota
        if (notaRecuperacao != null) {
            if (notaRecuperacao.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nota de recuperação não pode ser negativa");
            }
            if (avaliacao.getNotaMaxima() != null && notaRecuperacao.compareTo(avaliacao.getNotaMaxima()) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Nota de recuperação excede a nota máxima");
            }
        }

        BigDecimal notaAnterior = nota.getNotaRecuperacao();
        nota.setNotaRecuperacao(notaRecuperacao);
        nota.calcularNotaFinal();

        Nota saved = notaRepository.save(nota);

        // Registrar histórico
        if (notaAnterior == null || !notaAnterior.equals(notaRecuperacao)) {
            registrarHistorico(saved, notaAnterior, notaRecuperacao, "RECUPERACAO");
        }

        // Recalcular média
        if (nota.getMatriculaId() != null) {
            try {
                mediaService.recalcularAposAlteracaoNota(nota.getMatriculaId(), nota.getAvaliacaoId());
            } catch (Exception e) {
                log.warn("Erro ao recalcular média após nota de recuperação: matriculaId={}, avaliacaoId={}",
                        nota.getMatriculaId(), nota.getAvaliacaoId(), e);
            }
        }

        return NotaResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Nota n = notaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada"));

        // Registrar histórico
        registrarHistorico(n, n.getNota(), null, "EXCLUSAO");

        n.setDeleted(true);
        notaRepository.save(n);

        // Recalcular média após exclusão
        if (n.getMatriculaId() != null) {
            try {
                mediaService.recalcularAposAlteracaoNota(n.getMatriculaId(), n.getAvaliacaoId());
            } catch (Exception e) {
                log.warn("Erro ao recalcular média após exclusão de nota: matriculaId={}, avaliacaoId={}",
                        n.getMatriculaId(), n.getAvaliacaoId(), e);
            }
        }
    }

    private void registrarHistorico(Nota nota, BigDecimal notaAnterior, BigDecimal notaNova, String tipoAlteracao) {
        try {
            HistoricoNota historico = new HistoricoNota();
            historico.setTenantId(nota.getTenantId());
            historico.setNotaId(nota.getId());
            historico.setNotaAnterior(notaAnterior);
            historico.setNotaNova(notaNova);
            historico.setTipoAlteracao(tipoAlteracao);
            historico.setAlteradoPor(nota.getUpdatedBy() != null ? nota.getUpdatedBy() : nota.getTenantId());
            historico.setAlteradoEm(Instant.now());
            historicoNotaRepository.save(historico);
        } catch (Exception e) {
            log.warn("Erro ao registrar histórico de nota: notaId={}, tipoAlteracao={}", nota.getId(), tipoAlteracao, e);
        }
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
