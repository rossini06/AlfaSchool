package br.com.alfaschool.backend.application.frequencia;

import br.com.alfaschool.backend.application.diario.dto.FrequenciaLoteRequest;
import br.com.alfaschool.backend.application.frequencia.dto.FrequenciaRequest;
import br.com.alfaschool.backend.application.frequencia.dto.FrequenciaResponse;
import br.com.alfaschool.backend.domain.diario.StatusFrequencia;
import br.com.alfaschool.backend.domain.frequencia.Frequencia;
import br.com.alfaschool.backend.domain.matricula.Matricula;
import br.com.alfaschool.backend.domain.turma.Turma;
import br.com.alfaschool.backend.infrastructure.persistence.repository.FrequenciaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.MatriculaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TurmaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FrequenciaService {

    private final FrequenciaRepository frequenciaRepository;
    private final TurmaRepository turmaRepository;
    private final MatriculaRepository matriculaRepository;

    public FrequenciaService(
            FrequenciaRepository frequenciaRepository,
            TurmaRepository turmaRepository,
            MatriculaRepository matriculaRepository) {
        this.frequenciaRepository = frequenciaRepository;
        this.turmaRepository = turmaRepository;
        this.matriculaRepository = matriculaRepository;
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

    public List<FrequenciaResponse> listByTurmaAndDisciplinaAndData(UUID turmaId, UUID disciplinaId, LocalDate data) {
        UUID tenantId = requiredTenant();
        return frequenciaRepository.findByTenantIdAndTurmaIdAndDisciplinaIdAndDataAndDeletedFalse(
                        tenantId, turmaId, disciplinaId, data)
                .stream().map(FrequenciaResponse::from).toList();
    }

    @Transactional
    public FrequenciaResponse registrar(FrequenciaRequest request) {
        UUID tenantId = requiredTenant();

        // Validar turma
        Turma turma = turmaRepository.findById(request.turmaId())
                .filter(t -> tenantId.equals(t.getTenantId()) && !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        // Validar se data está dentro do período da turma
        if (turma.getDataInicio() != null && request.data().isBefore(turma.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data anterior ao início da turma");
        }
        if (turma.getDataFim() != null && request.data().isAfter(turma.getDataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data posterior ao fim da turma");
        }

        // Validar aluno matriculado e ativo
        if (request.matriculaId() != null) {
            validateMatriculaAtiva(tenantId, request.matriculaId());
        }

        // Verificar duplicidade (mesmo aluno + data + aula)
        Integer numeroAula = request.numeroAula() != null ? request.numeroAula() : 1;
        frequenciaRepository.findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndNumeroAulaAndDeletedFalse(
                        tenantId, request.alunoId(), request.turmaId(), request.disciplinaId(), request.data(), numeroAula)
                .ifPresent(f -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Frequência já registrada para este aluno/data/aula");
                });

        Frequencia f = new Frequencia();
        f.setTenantId(tenantId);
        f.setAlunoId(request.alunoId());
        f.setMatriculaId(request.matriculaId());
        f.setTurmaId(request.turmaId());
        f.setDisciplinaId(request.disciplinaId());
        f.setData(request.data());
        f.setNumeroAula(numeroAula);

        // Define status
        if (request.status() != null) {
            f.setStatus(request.status());
        } else {
            f.setStatus(request.presente() != null && request.presente()
                    ? StatusFrequencia.PRESENTE
                    : StatusFrequencia.AUSENTE);
        }

        f.setObs(request.obs());

        return FrequenciaResponse.from(frequenciaRepository.save(f));
    }

    /**
     * Registra frequência em lote para toda a turma de uma vez.
     */
    @Transactional
    public List<FrequenciaResponse> registrarLote(FrequenciaLoteRequest request) {
        UUID tenantId = requiredTenant();

        // Validar turma
        Turma turma = turmaRepository.findById(request.turmaId())
                .filter(t -> tenantId.equals(t.getTenantId()) && !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        // Validar período
        if (turma.getDataInicio() != null && request.data().isBefore(turma.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data anterior ao início da turma");
        }
        if (turma.getDataFim() != null && request.data().isAfter(turma.getDataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data posterior ao fim da turma");
        }

        Integer numeroAula = request.numeroAula() != null ? request.numeroAula() : 1;
        List<FrequenciaResponse> resultado = new ArrayList<>();

        for (FrequenciaLoteRequest.FrequenciaAlunoItem item : request.frequencias()) {
            // Verificar se já existe
            var existente = frequenciaRepository
                    .findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndNumeroAulaAndDeletedFalse(
                            tenantId, item.alunoId(), request.turmaId(), request.disciplinaId(),
                            request.data(), numeroAula);

            Frequencia f;
            if (existente.isPresent()) {
                // Atualiza existente
                f = existente.get();
            } else {
                // Cria nova
                f = new Frequencia();
                f.setTenantId(tenantId);
                f.setAlunoId(item.alunoId());
                f.setTurmaId(request.turmaId());
                f.setDisciplinaId(request.disciplinaId());
                f.setData(request.data());
                f.setNumeroAula(numeroAula);
            }

            f.setMatriculaId(item.matriculaId());
            f.setStatus(item.status() != null ? item.status() : StatusFrequencia.PRESENTE);
            f.setObs(item.obs());

            resultado.add(FrequenciaResponse.from(frequenciaRepository.save(f)));
        }

        return resultado;
    }

    /**
     * Marca todos os alunos como presentes para uma turma/disciplina/data.
     */
    @Transactional
    public List<FrequenciaResponse> marcarTodosPresentes(UUID turmaId, UUID disciplinaId, LocalDate data, Integer numeroAula) {
        UUID tenantId = requiredTenant();

        // Buscar todas as matrículas ativas da turma
        List<Matricula> matriculas = matriculaRepository.findByTenantIdAndTurmaIdAndStatusAndDeletedFalse(
                tenantId, turmaId, "ativa");

        if (matriculas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma matrícula ativa encontrada");
        }

        Integer aula = numeroAula != null ? numeroAula : 1;
        List<FrequenciaResponse> resultado = new ArrayList<>();

        for (Matricula m : matriculas) {
            var existente = frequenciaRepository
                    .findByTenantIdAndAlunoIdAndTurmaIdAndDisciplinaIdAndDataAndNumeroAulaAndDeletedFalse(
                            tenantId, m.getAlunoId(), turmaId, disciplinaId, data, aula);

            Frequencia f;
            if (existente.isPresent()) {
                f = existente.get();
            } else {
                f = new Frequencia();
                f.setTenantId(tenantId);
                f.setAlunoId(m.getAlunoId());
                f.setMatriculaId(m.getId());
                f.setTurmaId(turmaId);
                f.setDisciplinaId(disciplinaId);
                f.setData(data);
                f.setNumeroAula(aula);
            }

            f.setStatus(StatusFrequencia.PRESENTE);
            resultado.add(FrequenciaResponse.from(frequenciaRepository.save(f)));
        }

        return resultado;
    }

    @Transactional
    public FrequenciaResponse atualizar(UUID id, FrequenciaRequest request) {
        UUID tenantId = requiredTenant();
        Frequencia f = frequenciaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Frequência não encontrada"));

        if (request.status() != null) {
            f.setStatus(request.status());
        } else if (request.presente() != null) {
            f.setStatus(request.presente() ? StatusFrequencia.PRESENTE : StatusFrequencia.AUSENTE);
        }

        if (request.numeroAula() != null) {
            f.setNumeroAula(request.numeroAula());
        }

        f.setObs(request.obs());
        return FrequenciaResponse.from(frequenciaRepository.save(f));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Frequencia f = frequenciaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Frequência não encontrada"));
        f.setDeleted(true);
        frequenciaRepository.save(f);
    }

    private void validateMatriculaAtiva(UUID tenantId, UUID matriculaId) {
        matriculaRepository.findById(matriculaId)
                .filter(m -> tenantId.equals(m.getTenantId())
                        && !Boolean.TRUE.equals(m.getDeleted())
                        && "ativa".equalsIgnoreCase(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Matrícula não encontrada ou não está ativa"));
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
