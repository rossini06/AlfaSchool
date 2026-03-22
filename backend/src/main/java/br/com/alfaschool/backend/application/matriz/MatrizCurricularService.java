package br.com.alfaschool.backend.application.matriz;

import br.com.alfaschool.backend.application.matriz.dto.MatrizRequest;
import br.com.alfaschool.backend.application.matriz.dto.MatrizResponse;
import br.com.alfaschool.backend.domain.matriz.MatrizCurricular;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatrizCurricularRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class MatrizCurricularService {

    private final MatrizCurricularRepository matrizRepository;

    public MatrizCurricularService(MatrizCurricularRepository matrizRepository) {
        this.matrizRepository = matrizRepository;
    }

    public List<MatrizResponse> listByCurso(UUID cursoId) {
        UUID tenantId = requiredTenant();
        return matrizRepository.findByTenantIdAndCursoIdAndDeletedFalse(tenantId, cursoId)
                .stream().map(MatrizResponse::from).toList();
    }

    @Transactional
    public MatrizResponse create(MatrizRequest request) {
        UUID tenantId = requiredTenant();
        if (matrizRepository.existsByTenantIdAndCursoIdAndDisciplinaIdAndPeriodoAndDeletedFalse(
                tenantId, request.cursoId(), request.disciplinaId(), request.periodo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Disciplina já vinculada neste período do curso");
        }
        MatrizCurricular m = new MatrizCurricular();
        m.setTenantId(tenantId);
        m.setCursoId(request.cursoId());
        m.setDisciplinaId(request.disciplinaId());
        m.setPeriodo(request.periodo());
        m.setCargaHoraria(request.cargaHoraria());
        if (request.obrigatoria() != null) m.setObrigatoria(request.obrigatoria());
        return MatrizResponse.from(matrizRepository.save(m));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        MatrizCurricular m = matrizRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
        m.setDeleted(true);
        matrizRepository.save(m);
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
