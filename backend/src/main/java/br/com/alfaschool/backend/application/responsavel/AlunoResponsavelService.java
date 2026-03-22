package br.com.alfaschool.backend.application.responsavel;

import br.com.alfaschool.backend.application.responsavel.dto.AlunoResponsavelRequest;
import br.com.alfaschool.backend.application.responsavel.dto.AlunoResponsavelResponse;
import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelResponse;
import br.com.alfaschool.backend.domain.responsavel.AlunoResponsavel;
import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoResponsavelRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ResponsavelRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AlunoResponsavelService {

    private final AlunoResponsavelRepository alunoResponsavelRepository;
    private final ResponsavelRepository responsavelRepository;

    public AlunoResponsavelService(AlunoResponsavelRepository alunoResponsavelRepository,
                                   ResponsavelRepository responsavelRepository) {
        this.alunoResponsavelRepository = alunoResponsavelRepository;
        this.responsavelRepository = responsavelRepository;
    }

    public List<AlunoResponsavelResponse> listByAluno(UUID alunoId) {
        UUID tenantId = requiredTenant();
        return alunoResponsavelRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream()
                .map(link -> toResponse(link))
                .toList();
    }

    @Transactional
    public AlunoResponsavelResponse addLink(UUID alunoId, AlunoResponsavelRequest req) {
        UUID tenantId = requiredTenant();
        Responsavel responsavel = responsavelRepository.findById(req.responsavelId())
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));

        AlunoResponsavel link = new AlunoResponsavel();
        link.setTenantId(tenantId);
        link.setAlunoId(alunoId);
        link.setResponsavelId(req.responsavelId());
        applyRequest(link, req);

        AlunoResponsavel saved = alunoResponsavelRepository.save(link);
        return toResponse(saved, responsavel);
    }

    @Transactional
    public AlunoResponsavelResponse updateLink(UUID alunoId, UUID linkId, AlunoResponsavelRequest req) {
        UUID tenantId = requiredTenant();
        AlunoResponsavel link = alunoResponsavelRepository.findById(linkId)
                .filter(l -> tenantId.equals(l.getTenantId()) && !Boolean.TRUE.equals(l.getDeleted())
                        && alunoId.equals(l.getAlunoId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));

        applyRequest(link, req);
        AlunoResponsavel saved = alunoResponsavelRepository.save(link);
        return toResponse(saved);
    }

    @Transactional
    public void removeLink(UUID alunoId, UUID linkId) {
        UUID tenantId = requiredTenant();
        AlunoResponsavel link = alunoResponsavelRepository.findById(linkId)
                .filter(l -> tenantId.equals(l.getTenantId()) && !Boolean.TRUE.equals(l.getDeleted())
                        && alunoId.equals(l.getAlunoId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
        link.setDeleted(true);
        alunoResponsavelRepository.save(link);
    }

    private void applyRequest(AlunoResponsavel link, AlunoResponsavelRequest req) {
        if (req.parentesco() != null) link.setParentesco(req.parentesco());
        link.setParentescoDescricao(req.parentescoDescricao());
        if (req.responsavelFinanceiro() != null) link.setResponsavelFinanceiro(req.responsavelFinanceiro());
        if (req.responsavelAcademico() != null) link.setResponsavelAcademico(req.responsavelAcademico());
        if (req.autorizadoBuscar() != null) link.setAutorizadoBuscar(req.autorizadoBuscar());
        if (req.principal() != null) link.setPrincipal(req.principal());
        link.setObservacoes(req.observacoes());
    }

    private AlunoResponsavelResponse toResponse(AlunoResponsavel link) {
        Responsavel responsavel = responsavelRepository.findById(link.getResponsavelId()).orElse(null);
        return toResponse(link, responsavel);
    }

    private AlunoResponsavelResponse toResponse(AlunoResponsavel link, Responsavel responsavel) {
        ResponsavelResponse responsavelResponse = responsavel != null
                ? ResponsavelResponse.from(responsavel)
                : null;
        return new AlunoResponsavelResponse(
                link.getId(),
                link.getAlunoId(),
                link.getResponsavelId(),
                responsavelResponse,
                link.getParentesco(),
                link.getParentescoDescricao(),
                link.isResponsavelFinanceiro(),
                link.isResponsavelAcademico(),
                link.isAutorizadoBuscar(),
                link.isPrincipal(),
                link.getObservacoes()
        );
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
