package br.com.alfaschool.backend.application.matricula;

import br.com.alfaschool.backend.application.matricula.dto.MatriculaRequest;
import br.com.alfaschool.backend.application.matricula.dto.MatriculaResponse;
import br.com.alfaschool.backend.domain.matricula.Matricula;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatriculaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;

    public MatriculaService(MatriculaRepository matriculaRepository) {
        this.matriculaRepository = matriculaRepository;
    }

    public Page<MatriculaResponse> list(UUID alunoId, UUID turmaId, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (alunoId != null) {
            List<Matricula> result = matriculaRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId);
            return new org.springframework.data.domain.PageImpl<>(
                    result.stream().map(MatriculaResponse::from).toList(), pageable, result.size());
        }
        if (turmaId != null) {
            List<Matricula> result = matriculaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, turmaId);
            return new org.springframework.data.domain.PageImpl<>(
                    result.stream().map(MatriculaResponse::from).toList(), pageable, result.size());
        }
        return matriculaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(MatriculaResponse::from);
    }

    public MatriculaResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        return MatriculaResponse.from(matricula);
    }

    @Transactional
    public MatriculaResponse create(MatriculaRequest request) {
        UUID tenantId = requiredTenant();
        if (matriculaRepository.existsByTenantIdAndAlunoIdAndTurmaIdAndDeletedFalse(tenantId, request.alunoId(), request.turmaId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aluno já matriculado nesta turma");
        }
        Matricula matricula = new Matricula();
        matricula.setTenantId(tenantId);
        matricula.setAlunoId(request.alunoId());
        matricula.setTurmaId(request.turmaId());
        matricula.setUnitId(request.unitId());
        matricula.setDataMatricula(request.dataMatricula() != null ? request.dataMatricula() : LocalDate.now());
        matricula.setDataConclusao(request.dataConclusao());
        matricula.setObs(request.obs());
        matricula.setTipo(request.tipo() != null ? request.tipo() : "regular");
        matricula.setStatusAcademico(request.statusAcademico() != null ? request.statusAcademico() : "cursando");
        matricula.setDesconto(request.desconto());
        matricula.setStatus("ativa");
        matricula.setNumeroMatricula(generateNumero(tenantId));
        return MatriculaResponse.from(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse update(UUID id, MatriculaRequest request) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        matricula.setAlunoId(request.alunoId());
        matricula.setTurmaId(request.turmaId());
        matricula.setUnitId(request.unitId());
        if (request.dataMatricula() != null) {
            matricula.setDataMatricula(request.dataMatricula());
        }
        matricula.setDataConclusao(request.dataConclusao());
        matricula.setObs(request.obs());
        if (request.tipo() != null) matricula.setTipo(request.tipo());
        if (request.statusAcademico() != null) matricula.setStatusAcademico(request.statusAcademico());
        if (request.desconto() != null) matricula.setDesconto(request.desconto());
        return MatriculaResponse.from(matriculaRepository.save(matricula));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        matricula.setDeleted(true);
        matriculaRepository.save(matricula);
    }

    @Transactional
    public MatriculaResponse cancelar(UUID id) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        matricula.setStatus("cancelada");
        return MatriculaResponse.from(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse trancar(UUID id) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        matricula.setStatus("trancada");
        return MatriculaResponse.from(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse reativar(UUID id) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        matricula.setStatus("ativa");
        return MatriculaResponse.from(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse concluir(UUID id) {
        UUID tenantId = requiredTenant();
        Matricula matricula = matriculaRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()) && !Boolean.TRUE.equals(m.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));
        matricula.setStatus("concluida");
        matricula.setStatusAcademico("aprovado");
        if (matricula.getDataConclusao() == null) {
            matricula.setDataConclusao(LocalDate.now());
        }
        return MatriculaResponse.from(matriculaRepository.save(matricula));
    }

    @Transactional
    public MatriculaResponse updateStatus(UUID id, String novoStatus) {
        return switch (novoStatus) {
            case "ativa"     -> reativar(id);
            case "trancada"  -> trancar(id);
            case "cancelada" -> cancelar(id);
            case "concluida" -> concluir(id);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status inválido: " + novoStatus);
        };
    }

    private String generateNumero(UUID tenantId) {
        int ano = Year.now().getValue();
        String prefix = tenantId.toString().replace("-", "").substring(0, 4).toUpperCase();
        long count = matriculaRepository.count() + 1;
        return String.format("%d%s%05d", ano, prefix, count);
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        return tenantId;
    }
}
