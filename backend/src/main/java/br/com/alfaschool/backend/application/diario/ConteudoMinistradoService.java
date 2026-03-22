package br.com.alfaschool.backend.application.diario;

import br.com.alfaschool.backend.application.diario.dto.ConteudoMinistradoRequest;
import br.com.alfaschool.backend.application.diario.dto.ConteudoMinistradoResponse;
import br.com.alfaschool.backend.domain.diario.ConteudoMinistrado;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ConteudoMinistradoRepository;
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
public class ConteudoMinistradoService {

    private final ConteudoMinistradoRepository conteudoMinistradoRepository;

    public ConteudoMinistradoService(ConteudoMinistradoRepository conteudoMinistradoRepository) {
        this.conteudoMinistradoRepository = conteudoMinistradoRepository;
    }

    public Page<ConteudoMinistradoResponse> list(UUID turmaId, UUID disciplinaId, Pageable pageable) {
        UUID tenantId = requiredTenant();
        return conteudoMinistradoRepository
                .findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalse(tenantId, turmaId, disciplinaId, pageable)
                .map(ConteudoMinistradoResponse::from);
    }

    public List<ConteudoMinistradoResponse> listAll(UUID turmaId, UUID disciplinaId) {
        UUID tenantId = requiredTenant();
        return conteudoMinistradoRepository
                .findByTenantIdAndTurmaIdAndDisciplinaIdAndDeletedFalseOrderByDataDesc(tenantId, turmaId, disciplinaId)
                .stream()
                .map(ConteudoMinistradoResponse::from)
                .toList();
    }

    public ConteudoMinistradoResponse getById(UUID id) {
        UUID tenantId = requiredTenant();
        return conteudoMinistradoRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()) && !Boolean.TRUE.equals(c.getDeleted()))
                .map(ConteudoMinistradoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));
    }

    @Transactional
    public ConteudoMinistradoResponse create(ConteudoMinistradoRequest request) {
        UUID tenantId = requiredTenant();

        // Validar duplicidade (uma entrada por disciplina/dia)
        if (conteudoMinistradoRepository.existsByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
                tenantId, request.turmaId(), request.disciplinaId(), request.data())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe conteúdo registrado para esta disciplina nesta data");
        }

        ConteudoMinistrado c = new ConteudoMinistrado();
        c.setTenantId(tenantId);
        applyRequest(c, request);

        return ConteudoMinistradoResponse.from(conteudoMinistradoRepository.save(c));
    }

    @Transactional
    public ConteudoMinistradoResponse update(UUID id, ConteudoMinistradoRequest request) {
        UUID tenantId = requiredTenant();

        ConteudoMinistrado c = conteudoMinistradoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));

        // Verificar duplicidade se a data mudou
        if (!c.getData().equals(request.data())) {
            if (conteudoMinistradoRepository.existsByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalseAndIdNot(
                    tenantId, request.turmaId(), request.disciplinaId(), request.data(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Já existe conteúdo registrado para esta disciplina nesta data");
            }
        }

        applyRequest(c, request);
        return ConteudoMinistradoResponse.from(conteudoMinistradoRepository.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        ConteudoMinistrado c = conteudoMinistradoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));
        c.setDeleted(true);
        conteudoMinistradoRepository.save(c);
    }

    private void applyRequest(ConteudoMinistrado c, ConteudoMinistradoRequest request) {
        c.setTurmaId(request.turmaId());
        c.setDisciplinaId(request.disciplinaId());
        c.setProfessorId(request.professorId());
        c.setData(request.data());
        c.setDescricao(request.descricao());
        c.setObjetivos(request.objetivos());
        c.setRecursos(request.recursos());
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        return tenantId;
    }
}
