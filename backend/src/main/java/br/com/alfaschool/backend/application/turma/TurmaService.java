package br.com.alfaschool.backend.application.turma;

import br.com.alfaschool.backend.application.turma.dto.TurmaRequest;
import br.com.alfaschool.backend.application.turma.dto.TurmaResponse;
import br.com.alfaschool.backend.domain.turma.Turma;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TurmaRepository;
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
public class TurmaService {

    private final TurmaRepository turmaRepository;

    public TurmaService(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
    }

    public Page<TurmaResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (q != null && !q.isBlank()) {
            return turmaRepository.findByTenantIdAndNomeContainingIgnoreCaseAndDeletedFalse(tenantId, q, pageable)
                    .map(TurmaResponse::from);
        }
        return turmaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(TurmaResponse::from);
    }

    public List<TurmaResponse> listByCurso(UUID cursoId) {
        UUID tenantId = requiredTenant();
        return turmaRepository.findByTenantIdAndCursoIdAndDeletedFalse(tenantId, cursoId)
                .stream().map(TurmaResponse::from).toList();
    }

    public TurmaResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Turma turma = turmaRepository.findById(id)
                .filter(t -> tenantId.equals(t.getTenantId()) && !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        return TurmaResponse.from(turma);
    }

    @Transactional
    public TurmaResponse create(TurmaRequest request) {
        UUID tenantId = requiredTenant();
        Turma turma = new Turma();
        turma.setTenantId(tenantId);
        applyRequest(turma, request);
        return TurmaResponse.from(turmaRepository.save(turma));
    }

    @Transactional
    public TurmaResponse update(UUID id, TurmaRequest request) {
        UUID tenantId = requiredTenant();
        Turma turma = turmaRepository.findById(id)
                .filter(t -> tenantId.equals(t.getTenantId()) && !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        applyRequest(turma, request);
        return TurmaResponse.from(turmaRepository.save(turma));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Turma turma = turmaRepository.findById(id)
                .filter(t -> tenantId.equals(t.getTenantId()) && !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        turma.setDeleted(true);
        turmaRepository.save(turma);
    }

    private void applyRequest(Turma turma, TurmaRequest request) {
        if (request.dataInicio() != null && request.dataFim() != null
                && !request.dataInicio().isBefore(request.dataFim())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Data de início deve ser anterior à data de fim");
        }
        turma.setCursoId(request.cursoId());
        turma.setNome(request.nome());
        turma.setCodigo(request.codigo());
        turma.setAnoLetivo(request.anoLetivo());
        turma.setTurno(request.turno() != null ? request.turno() : "manha");
        turma.setProfessorResponsavel(request.professorResponsavel());
        turma.setCapacidadeMaxima(request.capacidadeMaxima() != null ? request.capacidadeMaxima() : 40);
        turma.setDataInicio(request.dataInicio());
        turma.setDataFim(request.dataFim());
        if (request.status() != null) turma.setStatus(request.status());
        turma.setUnitId(request.unitId());
        if (request.ativa() != null) {
            turma.setAtiva(request.ativa());
        }
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        return tenantId;
    }
}
