package br.com.alfaschool.backend.application.vinculo;

import br.com.alfaschool.backend.application.vinculo.dto.VinculoRequest;
import br.com.alfaschool.backend.application.vinculo.dto.VinculoResponse;
import br.com.alfaschool.backend.domain.vinculo.ProfessorTurmaDisciplina;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ProfessorTurmaDisciplinaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class VinculoService {

    private final ProfessorTurmaDisciplinaRepository vinculoRepository;

    public VinculoService(ProfessorTurmaDisciplinaRepository vinculoRepository) {
        this.vinculoRepository = vinculoRepository;
    }

    public List<VinculoResponse> listByTurma(UUID turmaId) {
        UUID tenantId = requiredTenant();
        return vinculoRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, turmaId)
                .stream().map(VinculoResponse::from).toList();
    }

    public List<VinculoResponse> listByProfessor(UUID professorId) {
        UUID tenantId = requiredTenant();
        return vinculoRepository.findByTenantIdAndProfessorIdAndDeletedFalse(tenantId, professorId)
                .stream().map(VinculoResponse::from).toList();
    }

    @Transactional
    public VinculoResponse create(VinculoRequest request) {
        UUID tenantId = requiredTenant();
        if (vinculoRepository.existsByTenantIdAndProfessorIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(
                tenantId, request.professorId(), request.turmaId(), request.disciplinaId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Professor já vinculado a esta disciplina/turma");
        }
        ProfessorTurmaDisciplina v = new ProfessorTurmaDisciplina();
        v.setTenantId(tenantId);
        v.setProfessorId(request.professorId());
        v.setTurmaId(request.turmaId());
        v.setDisciplinaId(request.disciplinaId());
        return VinculoResponse.from(vinculoRepository.save(v));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        ProfessorTurmaDisciplina v = vinculoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
        v.setDeleted(true);
        vinculoRepository.save(v);
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
