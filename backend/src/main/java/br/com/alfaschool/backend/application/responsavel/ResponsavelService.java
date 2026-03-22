package br.com.alfaschool.backend.application.responsavel;

import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelRequest;
import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelResponse;
import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ResponsavelRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ResponsavelService {

    private final ResponsavelRepository responsavelRepository;

    public ResponsavelService(ResponsavelRepository responsavelRepository) {
        this.responsavelRepository = responsavelRepository;
    }

    public List<ResponsavelResponse> listByAluno(UUID alunoId) {
        UUID tenantId = requiredTenant();
        return responsavelRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream().map(ResponsavelResponse::from).toList();
    }

    public ResponsavelResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        return responsavelRepository.findById(id)
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .map(ResponsavelResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
    }

    @Transactional
    public ResponsavelResponse create(ResponsavelRequest request) {
        UUID tenantId = requiredTenant();
        Responsavel r = new Responsavel();
        r.setTenantId(tenantId);
        applyRequest(r, request);
        return ResponsavelResponse.from(responsavelRepository.save(r));
    }

    @Transactional
    public ResponsavelResponse update(UUID id, ResponsavelRequest request) {
        UUID tenantId = requiredTenant();
        Responsavel r = responsavelRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
        applyRequest(r, request);
        return ResponsavelResponse.from(responsavelRepository.save(r));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Responsavel r = responsavelRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
        r.setDeleted(true);
        responsavelRepository.save(r);
    }

    private void applyRequest(Responsavel r, ResponsavelRequest req) {
        r.setAlunoId(req.alunoId());
        r.setNome(req.nome());
        r.setCpf(req.cpf());
        r.setTelefone(req.telefone());
        r.setEmail(req.email());
        if (req.tipo() != null) r.setTipo(req.tipo());
        if (req.principal() != null) r.setPrincipal(req.principal());
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
