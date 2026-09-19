package br.com.alfaschool.backend.application.responsavel;

import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelRequest;
import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelResponse;
import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ResponsavelRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.web.Paginacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ResponsavelService {

    private final ResponsavelRepository responsavelRepository;

    public ResponsavelService(ResponsavelRepository responsavelRepository) {
        this.responsavelRepository = responsavelRepository;
    }

    public List<ResponsavelResponse> listByAluno(UUID alunoId) {
        UUID tenantId = requiredTenant();
        return responsavelRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream().map(ResponsavelResponse::from).toList();
    }

    public Page<ResponsavelResponse> listAll(int page, int size, String search) {
        UUID tenantId = requiredTenant();
        Pageable pageable = Paginacao.de(page, size, Sort.by("nome").ascending());
        if (search != null && !search.isBlank()) {
            return responsavelRepository.searchByNome(tenantId, search, pageable)
                    .map(ResponsavelResponse::from);
        }
        return responsavelRepository.findByTenantIdAndDeletedFalse(tenantId, pageable)
                .map(ResponsavelResponse::from);
    }

    public ResponsavelResponse findById(UUID id) {
        UUID tenantId = requiredTenant();
        return responsavelRepository.findById(id)
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .map(ResponsavelResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
    }

    @Transactional
    public ResponsavelResponse create(ResponsavelRequest request) {
        UUID tenantId = requiredTenant();
        if (request.cpf() != null && !request.cpf().isBlank()
                && responsavelRepository.existsByTenantIdAndCpfAndDeletedFalse(tenantId, request.cpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um responsável com este CPF");
        }
        Responsavel r = new Responsavel();
        r.setTenantId(tenantId);
        applyRequest(r, request);
        return ResponsavelResponse.from(responsavelRepository.save(r));
    }

    @Transactional
    public ResponsavelResponse update(UUID id, ResponsavelRequest request) {
        UUID tenantId = requiredTenant();
        Responsavel r = responsavelRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
        if (request.cpf() != null && !request.cpf().isBlank()
                && responsavelRepository.existsByTenantIdAndCpfAndDeletedFalseAndIdNot(tenantId, request.cpf(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um responsável com este CPF");
        }
        applyRequest(r, request);
        return ResponsavelResponse.from(responsavelRepository.save(r));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requiredTenant();
        Responsavel r = responsavelRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
        r.setDeleted(true);
        responsavelRepository.save(r);
    }

    private void applyRequest(Responsavel r, ResponsavelRequest req) {
        r.setAlunoId(req.alunoId());
        r.setNome(req.nome());
        r.setCpf(req.cpf());
        r.setRg(req.rg());
        r.setDataNascimento(req.dataNascimento());
        r.setSexo(req.sexo());
        r.setEstadoCivil(req.estadoCivil());
        r.setProfissao(req.profissao());
        r.setEmpresa(req.empresa());
        r.setTelefone(req.telefone());
        r.setTelefone2(req.telefone2());
        r.setWhatsapp(req.whatsapp());
        r.setEmail(req.email());
        r.setEmailAlternativo(req.emailAlternativo());
        r.setLogradouro(req.logradouro());
        r.setNumeroEndereco(req.numeroEndereco());
        r.setComplemento(req.complemento());
        r.setBairro(req.bairro());
        r.setCidade(req.cidade());
        r.setEstado(req.estado());
        r.setCep(req.cep());
        r.setFoto(req.foto());
        r.setObservacoes(req.observacoes());
        if (req.tipo() != null) r.setTipo(req.tipo());
        if (req.principal() != null) r.setPrincipal(req.principal());
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
