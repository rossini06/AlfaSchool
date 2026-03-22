package br.com.alfaschool.backend.application.nota;

import br.com.alfaschool.backend.application.nota.dto.NotaRequest;
import br.com.alfaschool.backend.application.nota.dto.NotaResponse;
import br.com.alfaschool.backend.domain.nota.Nota;
import br.com.alfaschool.backend.infrastructure.persistence.repository.NotaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class NotaService {

    private final NotaRepository notaRepository;

    public NotaService(NotaRepository notaRepository) {
        this.notaRepository = notaRepository;
    }

    public List<NotaResponse> listByAluno(UUID alunoId) {
        UUID tenantId = requiredTenant();
        return notaRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream().map(NotaResponse::from).toList();
    }

    public List<NotaResponse> listByAvaliacao(UUID avaliacaoId) {
        UUID tenantId = requiredTenant();
        return notaRepository.findByTenantIdAndAvaliacaoIdAndDeletedFalse(tenantId, avaliacaoId)
                .stream().map(NotaResponse::from).toList();
    }

    @Transactional
    public NotaResponse lancar(NotaRequest request) {
        UUID tenantId = requiredTenant();
        // Upsert: atualiza se já existe, cria se não existe
        return notaRepository.findByTenantIdAndAlunoIdAndAvaliacaoIdAndDeletedFalse(
                tenantId, request.alunoId(), request.avaliacaoId())
                .map(existing -> {
                    existing.setNota(request.nota());
                    existing.setObs(request.obs());
                    return NotaResponse.from(notaRepository.save(existing));
                })
                .orElseGet(() -> {
                    Nota n = new Nota();
                    n.setTenantId(tenantId);
                    n.setAlunoId(request.alunoId());
                    n.setAvaliacaoId(request.avaliacaoId());
                    n.setNota(request.nota());
                    n.setObs(request.obs());
                    return NotaResponse.from(notaRepository.save(n));
                });
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Nota n = notaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada"));
        n.setDeleted(true);
        notaRepository.save(n);
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
