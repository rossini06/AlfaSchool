package br.com.alfaschool.backend.application.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.dto.SalaRequest;
import br.com.alfaschool.backend.application.access.estrutura.dto.SalaResponse;
import br.com.alfaschool.backend.domain.access.estrutura.AccSala;
import br.com.alfaschool.backend.domain.access.estrutura.AccZona;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccSalaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccTurmaSalaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccZonaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SalaService {

    private final AccSalaRepository salaRepository;
    private final AccZonaRepository zonaRepository;
    private final AccTurmaSalaRepository turmaSalaRepository;

    public SalaService(AccSalaRepository salaRepository,
                       AccZonaRepository zonaRepository,
                       AccTurmaSalaRepository turmaSalaRepository) {
        this.salaRepository = salaRepository;
        this.zonaRepository = zonaRepository;
        this.turmaSalaRepository = turmaSalaRepository;
    }

    public Page<SalaResponse> list(UUID unitId, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        Page<AccSala> pagina = unitId != null
                ? salaRepository.findByTenantIdAndUnitIdAndDeletedFalse(tenantId, unitId, pageable)
                : salaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return pagina.map(SalaResponse::from);
    }

    public List<SalaResponse> listByZona(UUID zonaId) {
        UUID tenantId = tenantObrigatorio();
        return salaRepository.findByTenantIdAndZonaIdAndDeletedFalse(tenantId, zonaId)
                .stream().map(SalaResponse::from).toList();
    }

    public SalaResponse findById(UUID id) {
        return SalaResponse.from(buscar(id));
    }

    @Transactional
    public SalaResponse create(SalaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccSala sala = new AccSala();
        sala.setTenantId(tenantId);
        sala.setUnitId(request.unitId());
        aplicar(sala, request, tenantId);
        return SalaResponse.from(salaRepository.save(sala));
    }

    @Transactional
    public SalaResponse update(UUID id, SalaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccSala sala = buscar(id);
        aplicar(sala, request, tenantId);
        return SalaResponse.from(salaRepository.save(sala));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccSala sala = buscar(id);
        long vinculos = turmaSalaRepository.countByTenantIdAndSalaIdAndDeletedFalse(tenantId, sala.getId());
        if (vinculos > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nao e possivel excluir: existem " + vinculos
                            + " vinculo(s) de turma nesta sala. Encerre a vigencia deles antes.");
        }
        sala.setDeleted(true);
        salaRepository.save(sala);
    }

    private void aplicar(AccSala sala, SalaRequest request, UUID tenantId) {
        if (request.zonaId() != null) {
            // Zona de outro tenant ou de outra unidade tornaria a sala inalcancavel
            // pelas regras de acesso, entao validamos aqui e nao so pela FK.
            AccZona zona = zonaRepository.findByIdAndTenantIdAndDeletedFalse(request.zonaId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona informada nao encontrada"));
            if (!zona.getUnitId().equals(request.unitId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A zona informada pertence a outra unidade");
            }
        }
        sala.setZonaId(request.zonaId());
        sala.setNome(request.nome().trim());
        sala.setCodigo(request.codigo());
        sala.setBloco(request.bloco());
        sala.setAndar(request.andar());
        sala.setCapacidade(request.capacidade());
        if (request.ativo() != null) {
            sala.setAtivo(request.ativo());
        }
    }

    private AccSala buscar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return salaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala nao encontrada"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        return tenantId;
    }
}
