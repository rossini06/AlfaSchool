package br.com.alfaschool.backend.application.unit;

import br.com.alfaschool.backend.application.unit.dto.UnitRequest;
import br.com.alfaschool.backend.application.unit.dto.UnitResponse;
import br.com.alfaschool.backend.domain.tenant.Unit;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UnitRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UnitService {

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public Page<UnitResponse> list(Pageable pageable) {
        UUID tenantId = requiredTenant();
        return unitRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(UnitResponse::from);
    }

    public UnitResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Unit unit = unitRepository.findById(id)
                .filter(u -> tenantId.equals(u.getTenantId()) && !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada"));
        return UnitResponse.from(unit);
    }

    @Transactional
    public UnitResponse create(UnitRequest request) {
        UUID tenantId = requiredTenant();
        Unit unit = new Unit();
        unit.setTenantId(tenantId);
        applyRequest(unit, request);
        return UnitResponse.from(unitRepository.save(unit));
    }

    @Transactional
    public UnitResponse update(UUID id, UnitRequest request) {
        UUID tenantId = requiredTenant();
        Unit unit = unitRepository.findById(id)
                .filter(u -> tenantId.equals(u.getTenantId()) && !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada"));
        applyRequest(unit, request);
        return UnitResponse.from(unitRepository.save(unit));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Unit unit = unitRepository.findById(id)
                .filter(u -> tenantId.equals(u.getTenantId()) && !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escola não encontrada"));
        unit.setDeleted(true);
        unitRepository.save(unit);
    }

    private void applyRequest(Unit unit, UnitRequest request) {
        unit.setName(request.name());
        unit.setAddress(request.address());
        unit.setCity(request.city());
        unit.setState(request.state());
        if (request.active() != null) {
            unit.setActive(request.active());
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
