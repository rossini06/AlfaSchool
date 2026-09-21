package br.com.alfaschool.backend.application.saas;

import br.com.alfaschool.backend.application.saas.dto.SaasMetricasResponse;
import br.com.alfaschool.backend.application.saas.dto.SaasPlanRequest;
import br.com.alfaschool.backend.application.saas.dto.SaasPlanResponse;
import br.com.alfaschool.backend.domain.saas.SaasPlan;
import br.com.alfaschool.backend.infrastructure.persistence.repository.SaasPlanRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SaasService {

    private final SaasPlanRepository saasPlanRepository;
    private final TenantRepository tenantRepository;

    public SaasService(SaasPlanRepository saasPlanRepository, TenantRepository tenantRepository) {
        this.saasPlanRepository = saasPlanRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<SaasPlanResponse> listPlans() {
        return saasPlanRepository.findByDeletedFalse().stream().map(SaasPlanResponse::from).toList();
    }

    @Transactional
    public SaasPlanResponse createPlan(SaasPlanRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        SaasPlan plan = new SaasPlan();
        plan.setTenantId(tenantId);
        applyRequest(plan, request);
        return SaasPlanResponse.from(saasPlanRepository.save(plan));
    }

    @Transactional
    public SaasPlanResponse updatePlan(UUID id, SaasPlanRequest request) {
        SaasPlan plan = saasPlanRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));
        applyRequest(plan, request);
        return SaasPlanResponse.from(saasPlanRepository.save(plan));
    }

    @Transactional
    public void deletePlan(UUID id) {
        SaasPlan plan = saasPlanRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));
        plan.setDeleted(true);
        saasPlanRepository.save(plan);
    }

    public SaasMetricasResponse metricas() {
        // Cross-tenant por definicao: com o filtro, "total de redes" dava 1.
        return TenantContext.semFiltro(() -> {
            long totalTenants = tenantRepository.count();
            long tenantsAtivos = tenantRepository.findAll().stream()
                    .filter(t -> t.isActive() && !Boolean.TRUE.equals(t.getDeleted()))
                    .count();
            long totalPlanos = saasPlanRepository.findByDeletedFalse().size();
            return new SaasMetricasResponse(totalTenants, tenantsAtivos, 0L, totalPlanos);
        });
    }

    private void applyRequest(SaasPlan plan, SaasPlanRequest request) {
        plan.setNome(request.nome());
        plan.setSlug(request.slug());
        plan.setDescricao(request.descricao());
        plan.setPrecoMensal(request.precoMensal() != null ? request.precoMensal() : BigDecimal.ZERO);
        plan.setPrecoAnual(request.precoAnual() != null ? request.precoAnual() : BigDecimal.ZERO);
        plan.setMaxEscolas(request.maxEscolas() != null ? request.maxEscolas() : -1);
        plan.setMaxUsuarios(request.maxUsuarios() != null ? request.maxUsuarios() : -1);
        plan.setMaxDispositivos(request.maxDispositivos() != null ? request.maxDispositivos() : -1);
        plan.setRecursos(request.recursos());
        if (request.ativo() != null) {
            plan.setAtivo(request.ativo());
        }
    }
}
