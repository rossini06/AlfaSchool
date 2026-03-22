package br.com.alfaschool.backend.application.tenant;

import br.com.alfaschool.backend.application.tenant.dto.TenantResponse;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Page<TenantResponse> list(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(TenantResponse::from);
    }

    public TenantResponse findById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rede de ensino não encontrada"));
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse changeStatus(UUID id, String status) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rede de ensino não encontrada"));
        boolean active = "ATIVO".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);
        tenant.setActive(active);
        return TenantResponse.from(tenantRepository.save(tenant));
    }
}
