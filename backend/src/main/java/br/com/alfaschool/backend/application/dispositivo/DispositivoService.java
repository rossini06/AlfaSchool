package br.com.alfaschool.backend.application.dispositivo;

import br.com.alfaschool.backend.application.dispositivo.dto.DispositivoRequest;
import br.com.alfaschool.backend.application.dispositivo.dto.DispositivoResponse;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;

    public DispositivoService(DispositivoRepository dispositivoRepository) {
        this.dispositivoRepository = dispositivoRepository;
    }

    public Page<DispositivoResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        Page<Dispositivo> pagina = (q == null || q.isBlank())
                ? dispositivoRepository.findByTenantIdAndDeletedFalse(tenantId, pageable)
                : dispositivoRepository.buscar(tenantId, q.trim(), pageable);
        return pagina.map(DispositivoResponse::from);
    }

    public DispositivoResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()) && !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado"));
        return DispositivoResponse.from(dispositivo);
    }

    @Transactional
    public DispositivoResponse create(DispositivoRequest request) {
        UUID tenantId = requiredTenant();
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setTenantId(tenantId);
        applyRequest(dispositivo, request);
        return DispositivoResponse.from(dispositivoRepository.save(dispositivo));
    }

    @Transactional
    public DispositivoResponse update(UUID id, DispositivoRequest request) {
        UUID tenantId = requiredTenant();
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()) && !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado"));
        applyRequest(dispositivo, request);
        return DispositivoResponse.from(dispositivoRepository.save(dispositivo));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()) && !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado"));
        dispositivo.setDeleted(true);
        dispositivoRepository.save(dispositivo);
    }

    @Transactional
    public DispositivoResponse ping(UUID id) {
        UUID tenantId = requiredTenant();
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()) && !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado"));
        dispositivo.setOnline(true);
        dispositivo.setUltimoPing(Instant.now());
        return DispositivoResponse.from(dispositivoRepository.save(dispositivo));
    }

    @Transactional
    public DispositivoResponse toggle(UUID id) {
        UUID tenantId = requiredTenant();
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()) && !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo não encontrado"));
        dispositivo.setAtivo(!dispositivo.isAtivo());
        return DispositivoResponse.from(dispositivoRepository.save(dispositivo));
    }

    private void applyRequest(Dispositivo dispositivo, DispositivoRequest request) {
        dispositivo.setNome(request.nome());
        dispositivo.setTipo(request.tipo() != null ? request.tipo() : "catraca");
        dispositivo.setFabricante(request.fabricante());
        dispositivo.setModelo(request.modelo());
        dispositivo.setIp(request.ip());
        dispositivo.setPorta(request.porta() != null ? request.porta() : 80);
        dispositivo.setSerial(request.serial());
        dispositivo.setApiToken(request.apiToken());
        dispositivo.setPortariaId(request.portariaId());
        // Os defaults da entidade valem quando o campo nao vem: um leitor
        // sem funcao ou sem sentido nao serve para a fila nem para a
        // apuracao, entao nao pode acabar nulo.
        if (request.funcao() != null) {
            dispositivo.setFuncao(request.funcao());
        }
        if (request.sentido() != null) {
            dispositivo.setSentido(request.sentido());
        }
        if (request.modoSync() != null) {
            dispositivo.setModoSync(request.modoSync());
        }
        dispositivo.setUnitId(request.unitId());
        if (request.ativo() != null) {
            dispositivo.setAtivo(request.ativo());
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
