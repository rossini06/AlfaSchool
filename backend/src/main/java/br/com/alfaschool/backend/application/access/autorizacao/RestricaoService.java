package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.RestricaoDocumentoResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.RestricaoRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.RestricaoResponse;
import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import br.com.alfaschool.backend.domain.access.autorizacao.TipoRestricao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRestricaoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Restricoes judiciais e administrativas.
 *
 * A restricao e' o freio do modulo: vence qualquer autorizacao. Por isso
 * aqui nao se "desativa por engano" — encerrar e' um ato explicito e a
 * chave do documento nunca sai junto com a listagem.
 */
@Service
public class RestricaoService {

    private final AccRestricaoRepository restricaoRepository;
    private final AccPessoaAutorizadaRepository pessoaRepository;
    private final AlunoRepository alunoRepository;

    public RestricaoService(AccRestricaoRepository restricaoRepository,
                            AccPessoaAutorizadaRepository pessoaRepository,
                            AlunoRepository alunoRepository) {
        this.restricaoRepository = restricaoRepository;
        this.pessoaRepository = pessoaRepository;
        this.alunoRepository = alunoRepository;
    }

    public Page<RestricaoResponse> list(Pageable pageable) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        return restricaoRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(RestricaoResponse::from);
    }

    public List<RestricaoResponse> listarPorAluno(UUID alunoId) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        return restricaoRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream().map(RestricaoResponse::from).toList();
    }

    public RestricaoResponse findById(UUID id) {
        return RestricaoResponse.from(carregar(id, ContextoAtual.tenantObrigatorio()));
    }

    /**
     * Unico caminho para a chave do documento restrito. O controller exige
     * perfil mais estrito; aqui so garantimos o isolamento por tenant.
     */
    public RestricaoDocumentoResponse documento(UUID id) {
        Restricao restricao = carregar(id, ContextoAtual.tenantObrigatorio());
        if (restricao.getDocumentoKey() == null || restricao.getDocumentoKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restrição não possui documento anexado");
        }
        return new RestricaoDocumentoResponse(restricao.getId(), restricao.getDocumentoKey());
    }

    @Transactional
    public RestricaoResponse create(RestricaoRequest request) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        Restricao restricao = new Restricao();
        restricao.setTenantId(tenantId);
        restricao.setRegistradoPorUserId(ContextoAtual.userIdAtual());
        aplicar(restricao, request, tenantId);
        return RestricaoResponse.from(restricaoRepository.save(restricao));
    }

    @Transactional
    public RestricaoResponse update(UUID id, RestricaoRequest request) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        Restricao restricao = carregar(id, tenantId);
        aplicar(restricao, request, tenantId);
        return RestricaoResponse.from(restricaoRepository.save(restricao));
    }

    /**
     * Encerrar = ativo false. Nao apagamos: a existencia da restricao e do
     * periodo em que ela valeu tem valor probatorio.
     */
    @Transactional
    public RestricaoResponse encerrar(UUID id) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        Restricao restricao = carregar(id, tenantId);
        restricao.setAtivo(false);
        return RestricaoResponse.from(restricaoRepository.save(restricao));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        Restricao restricao = carregar(id, tenantId);
        restricao.setDeleted(true);
        restricaoRepository.save(restricao);
    }

    private void aplicar(Restricao restricao, RestricaoRequest request, UUID tenantId) {
        alunoRepository.findById(request.alunoId())
                .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aluno não encontrado"));

        String cpf = CpfUtils.normalizar(request.pessoaCpf());
        if (cpf != null && !CpfUtils.valido(cpf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");
        }

        if (request.pessoaAutorizadaId() != null) {
            pessoaRepository.findByIdAndTenantIdAndDeletedFalse(request.pessoaAutorizadaId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Pessoa autorizada não encontrada"));
        } else if (cpf == null && (request.pessoaNome() == null || request.pessoaNome().isBlank())) {
            // Sem id, sem CPF e sem nome a restricao nao casaria com ninguem:
            // ficaria no cadastro dando falsa sensacao de protecao.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a pessoa autorizada ou ao menos nome e CPF da pessoa restrita");
        }

        restricao.setAlunoId(request.alunoId());
        restricao.setPessoaAutorizadaId(request.pessoaAutorizadaId());
        restricao.setPessoaNome(request.pessoaNome());
        restricao.setPessoaCpf(cpf);
        restricao.setTipo(request.tipo() != null ? request.tipo() : TipoRestricao.JUDICIAL);
        restricao.setDescricao(request.descricao());
        restricao.setDocumentoKey(request.documentoKey());
        restricao.setVigenciaInicio(request.vigenciaInicio());
        restricao.setVigenciaFim(request.vigenciaFim());

        if (request.vigenciaInicio() != null && request.vigenciaFim() != null
                && request.vigenciaFim().isBefore(request.vigenciaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fim de vigência não pode ser anterior ao início");
        }
        if (request.ativo() != null) {
            restricao.setAtivo(request.ativo());
        }
    }

    private Restricao carregar(UUID id, UUID tenantId) {
        return restricaoRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restrição não encontrada"));
    }
}
