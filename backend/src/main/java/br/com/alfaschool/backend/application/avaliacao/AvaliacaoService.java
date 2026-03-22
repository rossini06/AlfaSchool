package br.com.alfaschool.backend.application.avaliacao;

import br.com.alfaschool.backend.application.avaliacao.dto.AvaliacaoRequest;
import br.com.alfaschool.backend.application.avaliacao.dto.AvaliacaoResponse;
import br.com.alfaschool.backend.domain.avaliacao.Avaliacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AvaliacaoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Page<AvaliacaoResponse> list(Pageable pageable) {
        UUID tenantId = requiredTenant();
        return avaliacaoRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(AvaliacaoResponse::from);
    }

    public Page<AvaliacaoResponse> search(UUID turmaId, UUID disciplinaId, String periodo, String status, Pageable pageable) {
        UUID tenantId = requiredTenant();
        return avaliacaoRepository.search(tenantId, turmaId, disciplinaId, periodo, status, pageable)
                .map(AvaliacaoResponse::from);
    }

    public List<AvaliacaoResponse> listByTurma(UUID turmaId) {
        UUID tenantId = requiredTenant();
        return avaliacaoRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, turmaId)
                .stream().map(AvaliacaoResponse::from).toList();
    }

    public List<AvaliacaoResponse> listByTurmaAndDisciplina(UUID turmaId, UUID disciplinaId) {
        UUID tenantId = requiredTenant();
        return avaliacaoRepository.findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(tenantId, turmaId, disciplinaId)
                .stream().map(AvaliacaoResponse::from).toList();
    }

    public AvaliacaoResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Avaliacao a = avaliacaoRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
        return AvaliacaoResponse.from(a);
    }

    @Transactional
    public AvaliacaoResponse create(AvaliacaoRequest request) {
        UUID tenantId = requiredTenant();
        Avaliacao a = new Avaliacao();
        a.setTenantId(tenantId);
        applyRequest(a, request);
        return AvaliacaoResponse.from(avaliacaoRepository.save(a));
    }

    @Transactional
    public AvaliacaoResponse update(UUID id, AvaliacaoRequest request) {
        UUID tenantId = requiredTenant();
        Avaliacao a = avaliacaoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
        applyRequest(a, request);
        return AvaliacaoResponse.from(avaliacaoRepository.save(a));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Avaliacao a = avaliacaoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
        a.setDeleted(true);
        avaliacaoRepository.save(a);
    }

    private void applyRequest(Avaliacao a, AvaliacaoRequest req) {
        a.setTurmaId(req.turmaId());
        a.setDisciplinaId(req.disciplinaId());
        a.setNome(req.nome());
        if (req.tipo() != null) a.setTipo(req.tipo());
        a.setPeso(req.peso() != null ? req.peso() : BigDecimal.ONE);
        a.setDataAvaliacao(req.dataAvaliacao());
        a.setNotaMaxima(req.notaMaxima() != null ? req.notaMaxima() : BigDecimal.TEN);
        a.setPeriodo(req.periodo());
        a.setNotaMinima(req.notaMinima() != null ? req.notaMinima() : new BigDecimal("5.00"));
        if (req.status() != null) a.setStatus(req.status());
        a.setDescricao(req.descricao());
        a.setCriterios(req.criterios());
        a.setDataEntrega(req.dataEntrega());
        a.setPermiteRecuperacao(req.permiteRecuperacao() != null ? req.permiteRecuperacao() : true);
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
