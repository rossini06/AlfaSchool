package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaResponse;
import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ResponsavelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * CRUD das pessoas que podem se apresentar na portaria.
 *
 * Este service NAO decide se alguem pode retirar um aluno — isso e'
 * AutorizacaoConsultaService. Aqui so se cadastra a pessoa e as suas tres
 * permissoes independentes.
 */
@Service
public class PessoaAutorizadaService {

    private final AccPessoaAutorizadaRepository pessoaRepository;
    private final ResponsavelRepository responsavelRepository;

    public PessoaAutorizadaService(AccPessoaAutorizadaRepository pessoaRepository,
                                   ResponsavelRepository responsavelRepository) {
        this.pessoaRepository = pessoaRepository;
        this.responsavelRepository = responsavelRepository;
    }

    public Page<PessoaAutorizadaResponse> list(String q, Pageable pageable) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        Page<PessoaAutorizada> pagina = (q == null || q.isBlank())
                ? pessoaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable)
                : pessoaRepository.search(tenantId, q.trim(), pageable);
        return pagina.map(PessoaAutorizadaResponse::from);
    }

    public PessoaAutorizadaResponse findById(UUID id) {
        return PessoaAutorizadaResponse.from(carregar(id, ContextoAtual.tenantObrigatorio()));
    }

    @Transactional
    public PessoaAutorizadaResponse create(PessoaAutorizadaRequest request) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        PessoaAutorizada pessoa = new PessoaAutorizada();
        pessoa.setTenantId(tenantId);
        aplicar(pessoa, request, tenantId, null);
        return PessoaAutorizadaResponse.from(pessoaRepository.save(pessoa));
    }

    @Transactional
    public PessoaAutorizadaResponse update(UUID id, PessoaAutorizadaRequest request) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        PessoaAutorizada pessoa = carregar(id, tenantId);
        aplicar(pessoa, request, tenantId, id);
        return PessoaAutorizadaResponse.from(pessoaRepository.save(pessoa));
    }

    /**
     * Exclusao logica. Autorizacoes e restricoes existentes continuam
     * apontando para a linha; a verificacao de portaria exige
     * deleted=false, entao apagar a pessoa ja nega a retirada.
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        PessoaAutorizada pessoa = carregar(id, tenantId);
        pessoa.setDeleted(true);
        pessoaRepository.save(pessoa);
    }

    private void aplicar(PessoaAutorizada pessoa, PessoaAutorizadaRequest request, UUID tenantId, UUID idAtual) {
        if (request.responsavelId() != null) {
            // Vinculo opcional. Mesmo vinculado, NENHUMA das tres permissoes
            // e' herdada do cadastro de responsavel.
            responsavelRepository.findById(request.responsavelId())
                    .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsável não encontrado"));
        }
        pessoa.setResponsavelId(request.responsavelId());
        pessoa.setNome(request.nome().trim());

        String cpf = CpfUtils.normalizar(request.cpf());
        if (cpf != null) {
            if (!CpfUtils.valido(cpf)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");
            }
            boolean duplicado = idAtual == null
                    ? pessoaRepository.existsByTenantIdAndCpfAndDeletedFalse(tenantId, cpf)
                    : pessoaRepository.existsByTenantIdAndCpfAndDeletedFalseAndIdNot(tenantId, cpf, idAtual);
            if (duplicado) {
                // CPF duplicado quebraria o casamento de restricao por CPF solto:
                // a restricao acertaria uma linha e a outra continuaria liberada.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe pessoa autorizada com este CPF");
            }
        }
        pessoa.setCpf(cpf);
        pessoa.setRg(request.rg());
        pessoa.setTelefone(request.telefone());
        pessoa.setEmail(request.email());
        pessoa.setFotoKey(request.fotoKey());

        // As tres permissoes sao lidas SEPARADAMENTE do request. Nao derive
        // uma da outra nem do vinculo com responsavel.
        if (request.podeRetirar() != null) {
            pessoa.setPodeRetirar(request.podeRetirar());
        }
        if (request.podeAcessarPortal() != null) {
            pessoa.setPodeAcessarPortal(request.podeAcessarPortal());
        }
        if (request.recebeNotificacao() != null) {
            pessoa.setRecebeNotificacao(request.recebeNotificacao());
        }
        if (request.ativo() != null) {
            pessoa.setAtivo(request.ativo());
        }
    }

    private PessoaAutorizada carregar(UUID id, UUID tenantId) {
        return pessoaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa autorizada não encontrada"));
    }
}
