package br.com.alfaschool.backend.application.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.dto.PortariaRequest;
import br.com.alfaschool.backend.application.access.estrutura.dto.PortariaResponse;
import br.com.alfaschool.backend.domain.access.estrutura.AccPortaria;
import br.com.alfaschool.backend.domain.access.estrutura.TipoPortaria;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPortariaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PortariaService {

    private final AccPortariaRepository portariaRepository;

    public PortariaService(AccPortariaRepository portariaRepository) {
        this.portariaRepository = portariaRepository;
    }

    public Page<PortariaResponse> list(UUID unitId, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        Page<AccPortaria> pagina = unitId != null
                ? portariaRepository.findByTenantIdAndUnitIdAndDeletedFalse(tenantId, unitId, pageable)
                : portariaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return pagina.map(PortariaResponse::from);
    }

    public PortariaResponse findById(UUID id) {
        return PortariaResponse.from(buscar(id));
    }

    @Transactional
    public PortariaResponse create(PortariaRequest request) {
        UUID tenantId = tenantObrigatorio();
        if (portariaRepository.existsByTenantIdAndUnitIdAndNomeIgnoreCaseAndDeletedFalse(
                tenantId, request.unitId(), request.nome().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe uma portaria com esse nome nesta unidade");
        }
        AccPortaria portaria = new AccPortaria();
        portaria.setTenantId(tenantId);
        portaria.setUnitId(request.unitId());
        aplicar(portaria, request);
        return PortariaResponse.from(portariaRepository.save(portaria));
    }

    @Transactional
    public PortariaResponse update(UUID id, PortariaRequest request) {
        AccPortaria portaria = buscar(id);
        // unit_id nao muda: mover a portaria de unidade deixaria os eventos e
        // dispositivos ja gravados apontando para a unidade errada.
        aplicar(portaria, request);
        return PortariaResponse.from(portariaRepository.save(portaria));
    }

    @Transactional
    public void delete(UUID id) {
        AccPortaria portaria = buscar(id);
        long dispositivos = portariaRepository.contarDispositivosVinculados(portaria.getId());
        if (dispositivos > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nao e possivel excluir: existem " + dispositivos
                            + " dispositivo(s) vinculado(s) a esta portaria. "
                            + "Realoque ou remova os dispositivos antes.");
        }
        portaria.setDeleted(true);
        portariaRepository.save(portaria);
    }

    private void aplicar(AccPortaria portaria, PortariaRequest request) {
        portaria.setNome(request.nome().trim());
        portaria.setTipo(parseTipo(request.tipo()));
        portaria.setDescricao(request.descricao());
        if (request.ativo() != null) {
            portaria.setAtivo(request.ativo());
        }
    }

    private TipoPortaria parseTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return TipoPortaria.PRINCIPAL;
        }
        try {
            return TipoPortaria.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de portaria invalido. Valores aceitos: "
                            + Arrays.stream(TipoPortaria.values()).map(Enum::name)
                            .collect(Collectors.joining(", ")));
        }
    }

    private AccPortaria buscar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return portariaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portaria nao encontrada"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        return tenantId;
    }
}
