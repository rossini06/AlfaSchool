package br.com.alfaschool.backend.application.curso;

import br.com.alfaschool.backend.application.curso.dto.CursoRequest;
import br.com.alfaschool.backend.application.curso.dto.CursoResponse;
import br.com.alfaschool.backend.domain.curso.Curso;
import br.com.alfaschool.backend.infrastructure.persistence.repository.CursoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public Page<CursoResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (q != null && !q.isBlank()) {
            return cursoRepository.findByTenantIdAndNomeContainingIgnoreCaseAndDeletedFalse(tenantId, q, pageable)
                    .map(CursoResponse::from);
        }
        return cursoRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(CursoResponse::from);
    }

    public CursoResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Curso curso = cursoRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso não encontrado"));
        return CursoResponse.from(curso);
    }

    @Transactional
    public CursoResponse create(CursoRequest request) {
        UUID tenantId = requiredTenant();
        Curso curso = new Curso();
        curso.setTenantId(tenantId);
        applyRequest(curso, request);
        return CursoResponse.from(cursoRepository.save(curso));
    }

    @Transactional
    public CursoResponse update(UUID id, CursoRequest request) {
        UUID tenantId = requiredTenant();
        Curso curso = cursoRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso não encontrado"));
        applyRequest(curso, request);
        return CursoResponse.from(cursoRepository.save(curso));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Curso curso = cursoRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso não encontrado"));
        curso.setDeleted(true);
        cursoRepository.save(curso);
    }

    private void applyRequest(Curso curso, CursoRequest request) {
        curso.setNome(request.nome());
        curso.setCodigo(request.codigo());
        curso.setDescricao(request.descricao());
        curso.setCargaHoraria(request.cargaHoraria());
        curso.setModalidade(request.modalidade() != null ? request.modalidade() : "presencial");
        curso.setNivel(request.nivel());
        curso.setUnitId(request.unitId());
        if (request.ativo() != null) {
            curso.setAtivo(request.ativo());
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
