package br.com.alfaschool.backend.application.aluno;

import br.com.alfaschool.backend.application.access.biometria.FotoStorage;
import br.com.alfaschool.backend.application.access.biometria.FotoUrlAssinada;
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

import java.util.Base64;
import java.util.UUID;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final FotoStorage fotoStorage;
    private final FotoUrlAssinada fotoUrlAssinada;

    public AlunoService(AlunoRepository alunoRepository, FotoStorage fotoStorage, FotoUrlAssinada fotoUrlAssinada) {
        this.alunoRepository = alunoRepository;
        this.fotoStorage = fotoStorage;
        this.fotoUrlAssinada = fotoUrlAssinada;
    }

    public Page<AlunoResponse> list(String q, Pageable pageable) {
        UUID tenantId = requiredTenant();
        if (q != null && !q.isBlank()) {
            return alunoRepository.search(tenantId, q, pageable).map(a -> AlunoResponse.from(a, fotoUrlAssinada));
        }
        return alunoRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(a -> AlunoResponse.from(a, fotoUrlAssinada));
    }

    public AlunoResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        Aluno aluno = alunoRepository.findById(id)
                .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        return AlunoResponse.from(aluno, fotoUrlAssinada);
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
        return AlunoResponse.from(alunoRepository.save(aluno), fotoUrlAssinada);
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
        return AlunoResponse.from(alunoRepository.save(aluno), fotoUrlAssinada);
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
        aplicarFoto(aluno, request.foto());
        aluno.setObservacoesMedicas(request.observacoesMedicas());
        aluno.setUnitId(request.unitId());
        if (request.ativo() != null) {
            aluno.setAtivo(request.ativo());
        }
    }

    /**
     * A foto entra pelo corpo como data-URI base64 (como o cadastro sempre
     * mandou) e sai gravada no FotoStorage; a linha guarda so' a chave.
     *
     * null = edicao que nao mexe na foto, preserva a atual. Em branco =
     * remover. Data-URI/base64 = nova foto (valida formato e tamanho no
     * FotoStorage.salvar e apaga a anterior).
     */
    private void aplicarFoto(Aluno aluno, String fotoEntrada) {
        if (fotoEntrada == null) {
            return;
        }
        if (fotoEntrada.isBlank()) {
            if (aluno.getFotoKey() != null) {
                fotoStorage.remover(aluno.getFotoKey());
                aluno.setFotoKey(null);
            }
            aluno.setFoto(null);
            return;
        }
        byte[] bytes = decodificarBase64(fotoEntrada);
        String chaveAntiga = aluno.getFotoKey();
        String chave;
        try {
            chave = fotoStorage.salvar(aluno.getTenantId(), bytes);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        aluno.setFotoKey(chave);
        aluno.setFoto(null); // nao guarda mais base64 na linha do aluno
        if (chaveAntiga != null && !chaveAntiga.equals(chave)) {
            fotoStorage.remover(chaveAntiga);
        }
    }

    /** Aceita "data:image/...;base64,XXXX" ou base64 puro. */
    private static byte[] decodificarBase64(String valor) {
        String limpo = valor;
        int virgula = limpo.indexOf(',');
        if (limpo.startsWith("data:") && virgula > 0) {
            limpo = limpo.substring(virgula + 1);
        }
        limpo = limpo.replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(limpo);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto em base64 inválida.");
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
