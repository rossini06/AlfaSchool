package br.com.alfaschool.backend.application.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.dto.ZonaRequest;
import br.com.alfaschool.backend.application.access.estrutura.dto.ZonaResponse;
import br.com.alfaschool.backend.domain.access.estrutura.AccZona;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccSalaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccZonaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ZonaService {

    private final AccZonaRepository zonaRepository;
    private final AccSalaRepository salaRepository;

    public ZonaService(AccZonaRepository zonaRepository, AccSalaRepository salaRepository) {
        this.zonaRepository = zonaRepository;
        this.salaRepository = salaRepository;
    }

    public Page<ZonaResponse> list(UUID unitId, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        Page<AccZona> pagina = unitId != null
                ? zonaRepository.findByTenantIdAndUnitIdAndDeletedFalse(tenantId, unitId, pageable)
                : zonaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return pagina.map(ZonaResponse::from);
    }

    public ZonaResponse findById(UUID id) {
        return ZonaResponse.from(buscar(id));
    }

    @Transactional
    public ZonaResponse create(ZonaRequest request) {
        UUID tenantId = tenantObrigatorio();
        if (zonaRepository.existsByTenantIdAndUnitIdAndNomeIgnoreCaseAndDeletedFalse(
                tenantId, request.unitId(), request.nome().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe uma zona com esse nome nesta unidade");
        }
        AccZona zona = new AccZona();
        zona.setTenantId(tenantId);
        zona.setUnitId(request.unitId());
        aplicar(zona, request);
        return ZonaResponse.from(zonaRepository.save(zona));
    }

    @Transactional
    public ZonaResponse update(UUID id, ZonaRequest request) {
        AccZona zona = buscar(id);
        aplicar(zona, request);
        return ZonaResponse.from(zonaRepository.save(zona));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccZona zona = buscar(id);
        // Sala aponta para zona por FK; apagar a zona deixaria salas orfas.
        long salas = salaRepository.countByTenantIdAndZonaIdAndDeletedFalse(tenantId, zona.getId());
        if (salas > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nao e possivel excluir: existem " + salas + " sala(s) nesta zona");
        }
        zona.setDeleted(true);
        zonaRepository.save(zona);
    }

    private void aplicar(AccZona zona, ZonaRequest request) {
        zona.setNome(request.nome().trim());
        zona.setDescricao(request.descricao());
        if (request.ativo() != null) {
            zona.setAtivo(request.ativo());
        }
    }

    private AccZona buscar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return zonaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona nao encontrada"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        return tenantId;
    }
}
