package br.com.alfaschool.backend.application.frequencia;

import br.com.alfaschool.backend.application.frequencia.dto.FrequenciaRequest;
import br.com.alfaschool.backend.application.frequencia.dto.FrequenciaResponse;
import br.com.alfaschool.backend.domain.frequencia.Frequencia;
import br.com.alfaschool.backend.infrastructure.persistence.repository.FrequenciaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FrequenciaService {

    private final FrequenciaRepository frequenciaRepository;

    public FrequenciaService(FrequenciaRepository frequenciaRepository) {
        this.frequenciaRepository = frequenciaRepository;
    }

    public List<FrequenciaResponse> listByAluno(UUID alunoId) {
        UUID tenantId = requiredTenant();
        return frequenciaRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream().map(FrequenciaResponse::from).toList();
    }

    public List<FrequenciaResponse> listByTurmaAndDisciplina(UUID turmaId, UUID disciplinaId, LocalDate inicio, LocalDate fim) {
        UUID tenantId = requiredTenant();
        return frequenciaRepository.findByTurmaAndDisciplinaAndPeriodo(tenantId, turmaId, disciplinaId, inicio, fim)
                .stream().map(FrequenciaResponse::from).toList();
    }

    @Transactional
    public FrequenciaResponse registrar(FrequenciaRequest request) {
        UUID tenantId = requiredTenant();
        frequenciaRepository.findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
                tenantId, request.alunoId(), request.turmaId(), request.disciplinaId(), request.data())
                .ifPresent(f -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Frequência já registrada para este dia");
                });
        Frequencia f = new Frequencia();
        f.setTenantId(tenantId);
        f.setAlunoId(request.alunoId());
        f.setTurmaId(request.turmaId());
        f.setDisciplinaId(request.disciplinaId());
        f.setData(request.data());
        f.setPresente(request.presente() != null ? request.presente() : true);
        f.setObs(request.obs());
        return FrequenciaResponse.from(frequenciaRepository.save(f));
    }

    @Transactional
    public FrequenciaResponse atualizar(UUID id, FrequenciaRequest request) {
        UUID tenantId = requiredTenant();
        Frequencia f = frequenciaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Frequência não encontrada"));
        if (request.presente() != null) f.setPresente(request.presente());
        f.setObs(request.obs());
        return FrequenciaResponse.from(frequenciaRepository.save(f));
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
