package br.com.alfaschool.backend.application.disciplina;

import br.com.alfaschool.backend.application.disciplina.dto.DisciplinaRequest;
import br.com.alfaschool.backend.application.disciplina.dto.DisciplinaResponse;
import br.com.alfaschool.backend.domain.disciplina.Disciplina;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DisciplinaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;

    public DisciplinaService(DisciplinaRepository disciplinaRepository) {
        this.disciplinaRepository = disciplinaRepository;
    }

    public Page<DisciplinaResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (q != null && !q.isBlank()) {
            return disciplinaRepository.search(tenantId, q, pageable).map(DisciplinaResponse::from);
        }
        return disciplinaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(DisciplinaResponse::from);
    }

    public List<DisciplinaResponse> listAtivas() {
        UUID tenantId = requiredTenant();
        return disciplinaRepository.findByTenantIdAndDeletedFalseAndAtivaTrue(tenantId)
                .stream().map(DisciplinaResponse::from).toList();
    }

    public DisciplinaResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        return disciplinaRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()) && !Boolean.TRUE.equals(d.getDeleted()))
                .map(DisciplinaResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disciplina não encontrada"));
    }

    @Transactional
    public DisciplinaResponse create(DisciplinaRequest request) {
        UUID tenantId = requiredTenant();
        if (disciplinaRepository.existsByTenantIdAndNomeIgnoreCaseAndDeletedFalse(tenantId, request.nome())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma disciplina com este nome");
        }
        Disciplina d = new Disciplina();
        d.setTenantId(tenantId);
        applyRequest(d, request);
        return DisciplinaResponse.from(disciplinaRepository.save(d));
    }

    @Transactional
    public DisciplinaResponse update(UUID id, DisciplinaRequest request) {
        UUID tenantId = requiredTenant();
        Disciplina d = disciplinaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disciplina não encontrada"));
        if (disciplinaRepository.existsByTenantIdAndNomeIgnoreCaseAndDeletedFalseAndIdNot(tenantId, request.nome(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma disciplina com este nome");
        }
        applyRequest(d, request);
        return DisciplinaResponse.from(disciplinaRepository.save(d));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Disciplina d = disciplinaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disciplina não encontrada"));
        d.setDeleted(true);
        disciplinaRepository.save(d);
    }

    private void applyRequest(Disciplina d, DisciplinaRequest req) {
        d.setNome(req.nome());
        d.setCodigo(req.codigo());
        d.setCargaHoraria(req.cargaHoraria());
        d.setDescricao(req.descricao());
        d.setCursoId(req.cursoId());
        d.setTipo(req.tipo());
        d.setNotaMaxima(req.notaMaxima());
        d.setPeso(req.peso());
        if (req.permiteRecuperacao() != null) d.setPermiteRecuperacao(req.permiteRecuperacao());
        d.setTipoAvaliacao(req.tipoAvaliacao());
        if (req.ativa() != null) d.setAtiva(req.ativa());
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
