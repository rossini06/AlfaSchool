package br.com.alfaschool.backend.application.professor;

import br.com.alfaschool.backend.application.professor.dto.ProfessorRequest;
import br.com.alfaschool.backend.application.professor.dto.ProfessorResponse;
import br.com.alfaschool.backend.domain.professor.Professor;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ProfessorRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;

    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    public Page<ProfessorResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (q != null && !q.isBlank()) {
            return professorRepository.search(tenantId, q, pageable).map(ProfessorResponse::from);
        }
        return professorRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(ProfessorResponse::from);
    }

    public ProfessorResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        return professorRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()) && !Boolean.TRUE.equals(p.getDeleted()))
                .map(ProfessorResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado"));
    }

    @Transactional
    public ProfessorResponse create(ProfessorRequest request) {
        UUID tenantId = requiredTenant();
        if (request.cpf() != null && !request.cpf().isBlank()) {
            professorRepository.findByTenantIdAndCpfAndDeletedFalse(tenantId, request.cpf()).ifPresent(p -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado para este professor");
            });
        }
        Professor professor = new Professor();
        professor.setTenantId(tenantId);
        applyRequest(professor, request);
        return ProfessorResponse.from(professorRepository.save(professor));
    }

    @Transactional
    public ProfessorResponse update(UUID id, ProfessorRequest request) {
        UUID tenantId = requiredTenant();
        Professor professor = professorRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()) && !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado"));
        if (request.cpf() != null && !request.cpf().isBlank() && !request.cpf().equals(professor.getCpf())) {
            professorRepository.findByTenantIdAndCpfAndDeletedFalse(tenantId, request.cpf()).ifPresent(p -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado para este professor");
            });
        }
        applyRequest(professor, request);
        return ProfessorResponse.from(professorRepository.save(professor));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Professor professor = professorRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()) && !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado"));
        professor.setDeleted(true);
        professorRepository.save(professor);
    }

    private void applyRequest(Professor p, ProfessorRequest req) {
        p.setNome(req.nome());
        p.setCpf(req.cpf());
        p.setEmail(req.email());
        p.setTelefone(req.telefone());
        p.setEspecialidade(req.especialidade());
        p.setDataNascimento(req.dataNascimento());
        p.setDataContratacao(req.dataContratacao());
        p.setUnitId(req.unitId());
        if (req.status() != null) p.setStatus(req.status());
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
