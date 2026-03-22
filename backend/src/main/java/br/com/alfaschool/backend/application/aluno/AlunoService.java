package br.com.alfaschool.backend.application.aluno;

import br.com.alfaschool.backend.application.aluno.dto.AlunoRequest;
import br.com.alfaschool.backend.application.aluno.dto.AlunoResponse;
import br.com.alfaschool.backend.domain.aluno.Aluno;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;

    public AlunoService(AlunoRepository alunoRepository) {
        this.alunoRepository = alunoRepository;
    }

    public Page<AlunoResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (q != null && !q.isBlank()) {
            return alunoRepository.search(tenantId, q, pageable).map(AlunoResponse::from);
        }
        return alunoRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(AlunoResponse::from);
    }

    public AlunoResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Aluno aluno = alunoRepository.findById(id)
                .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        return AlunoResponse.from(aluno);
    }

    @Transactional
    public AlunoResponse create(AlunoRequest request) {
        UUID tenantId = requiredTenant();
        if (request.cpf() != null && !request.cpf().isBlank()) {
            alunoRepository.findByTenantIdAndCpfAndDeletedFalse(tenantId, request.cpf()).ifPresent(a -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado para este tenant");
            });
        }
        Aluno aluno = new Aluno();
        aluno.setTenantId(tenantId);
        applyRequest(aluno, request);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional
    public AlunoResponse update(UUID id, AlunoRequest request) {
        UUID tenantId = requiredTenant();
        Aluno aluno = alunoRepository.findById(id)
                .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        if (request.cpf() != null && !request.cpf().isBlank() && !request.cpf().equals(aluno.getCpf())) {
            alunoRepository.findByTenantIdAndCpfAndDeletedFalse(tenantId, request.cpf()).ifPresent(a -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado para este tenant");
            });
        }
        applyRequest(aluno, request);
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Aluno aluno = alunoRepository.findById(id)
                .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        aluno.setDeleted(true);
        alunoRepository.save(aluno);
    }

    private void applyRequest(Aluno aluno, AlunoRequest request) {
        aluno.setNome(request.nome());
        aluno.setCpf(request.cpf());
        aluno.setRg(request.rg());
        aluno.setEmail(request.email());
        aluno.setTelefone(request.telefone());
        aluno.setDataNascimento(request.dataNascimento());
        aluno.setSexo(request.sexo());
        aluno.setEndereco(request.endereco());
        aluno.setCidade(request.cidade());
        aluno.setEstado(request.estado());
        aluno.setCep(request.cep());
        aluno.setNomeResponsavel(request.nomeResponsavel());
        aluno.setTelefoneResponsavel(request.telefoneResponsavel());
        aluno.setEmailResponsavel(request.emailResponsavel());
        aluno.setFoto(request.foto());
        aluno.setUnitId(request.unitId());
        if (request.ativo() != null) {
            aluno.setAtivo(request.ativo());
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
