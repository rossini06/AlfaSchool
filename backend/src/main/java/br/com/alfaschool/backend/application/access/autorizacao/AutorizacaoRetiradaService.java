package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoHistoricoResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoRetiradaRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoRetiradaResponse;
import br.com.alfaschool.backend.domain.access.autorizacao.AcaoAutorizacao;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import br.com.alfaschool.backend.domain.access.shared.OrigemAutorizacao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoHistoricoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAutorizacaoRetiradaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPessoaAutorizadaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida da autorizacao de retirada.
 *
 * Duas invariantes que este service existe para proteger:
 *
 * 1. PORTAL NUNCA NASCE VALIDA. Um pedido feito pelo responsavel no portal
 *    nasce PENDENTE, sem excecao, e so vira ATIVA quando um usuario da
 *    escola aprova. Se isso for afrouxado, qualquer um que consiga uma conta
 *    de portal se autoriza a buscar a crianca.
 *
 * 2. TEMPORARIA EXIGE FIM. permanente=false sem vigenciaFim seria uma
 *    autorizacao de uma sexta-feira valendo para sempre. Rejeitamos na
 *    criacao e na edicao.
 *
 * Toda transicao de status grava acc_autorizacao_historico. O historico e'
 * append-only: so inserimos.
 */
@Service
public class AutorizacaoRetiradaService {

    private final AccAutorizacaoRetiradaRepository autorizacaoRepository;
    private final AccAutorizacaoHistoricoRepository historicoRepository;
    private final AccPessoaAutorizadaRepository pessoaRepository;
    private final AlunoRepository alunoRepository;

    public AutorizacaoRetiradaService(AccAutorizacaoRetiradaRepository autorizacaoRepository,
                                      AccAutorizacaoHistoricoRepository historicoRepository,
                                      AccPessoaAutorizadaRepository pessoaRepository,
                                      AlunoRepository alunoRepository) {
        this.autorizacaoRepository = autorizacaoRepository;
        this.historicoRepository = historicoRepository;
        this.pessoaRepository = pessoaRepository;
        this.alunoRepository = alunoRepository;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<AutorizacaoRetiradaResponse> listarPorAluno(UUID alunoId) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        return autorizacaoRepository.findByTenantIdAndAlunoIdAndDeletedFalse(tenantId, alunoId)
                .stream()
                .sorted(Comparator.comparing(AutorizacaoRetirada::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(AutorizacaoRetiradaResponse::from)
                .toList();
    }

    public List<AutorizacaoRetiradaResponse> listarPorPessoa(UUID pessoaAutorizadaId) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        return autorizacaoRepository.findByTenantIdAndPessoaAutorizadaIdAndDeletedFalse(tenantId, pessoaAutorizadaId)
                .stream()
                .sorted(Comparator.comparing(AutorizacaoRetirada::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(AutorizacaoRetiradaResponse::from)
                .toList();
    }

    public AutorizacaoRetiradaResponse findById(UUID id) {
        return AutorizacaoRetiradaResponse.from(carregar(id, ContextoAtual.tenantObrigatorio()));
    }

    public List<AutorizacaoHistoricoResponse> historico(UUID autorizacaoId) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        carregar(autorizacaoId, tenantId);
        return historicoRepository.findByTenantIdAndAutorizacaoIdOrderByCreatedAtAsc(tenantId, autorizacaoId)
                .stream()
                .map(AutorizacaoHistoricoResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Transactional
    public AutorizacaoRetiradaResponse criar(AutorizacaoRetiradaRequest request, String ip) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();

        alunoRepository.findById(request.alunoId())
                .filter(a -> tenantId.equals(a.getTenantId()) && !Boolean.TRUE.equals(a.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aluno não encontrado"));

        PessoaAutorizada pessoa = pessoaRepository
                .findByIdAndTenantIdAndDeletedFalse(request.pessoaAutorizadaId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pessoa autorizada não encontrada"));

        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        autorizacao.setTenantId(tenantId);
        autorizacao.setAlunoId(request.alunoId());
        autorizacao.setPessoaAutorizadaId(pessoa.getId());
        aplicarJanela(autorizacao, request);
        autorizacao.setMotivo(request.motivo());
        autorizacao.setDocumentoKey(request.documentoKey());
        autorizacao.setObservacao(request.observacao());

        OrigemAutorizacao origem = request.origem() != null ? request.origem() : OrigemAutorizacao.ESCOLA;
        autorizacao.setOrigem(origem);

        if (origem == OrigemAutorizacao.PORTAL) {
            // Pedido do responsavel: nasce PENDENTE e NAO vale. Quem libera e'
            // a escola, via aprovar(). Nao ha parametro que contorne isso.
            autorizacao.setStatus(StatusAutorizacao.PENDENTE);
        } else {
            // Criada por usuario autenticado da escola: ja nasce valendo.
            autorizacao.setStatus(StatusAutorizacao.ATIVA);
            autorizacao.setAprovadoPorUserId(ContextoAtual.userIdAtual());
            autorizacao.setAprovadoEm(Instant.now());
        }

        AutorizacaoRetirada salva = autorizacaoRepository.save(autorizacao);
        gravarHistorico(salva, AcaoAutorizacao.CRIACAO, null, salva.getStatus(),
                ContextoAtual.userIdAtual(), request.motivo(), ip);
        return AutorizacaoRetiradaResponse.from(salva);
    }

    /**
     * Edicao dos dados da janela. Nao muda status: para isso existem as
     * transicoes proprias, que exigem motivo e geram historico.
     */
    @Transactional
    public AutorizacaoRetiradaResponse atualizar(UUID id, AutorizacaoRetiradaRequest request) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        AutorizacaoRetirada autorizacao = carregar(id, tenantId);
        if (autorizacao.getStatus() == StatusAutorizacao.REVOGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Autorização revogada não pode ser editada; crie uma nova");
        }
        aplicarJanela(autorizacao, request);
        autorizacao.setDocumentoKey(request.documentoKey());
        autorizacao.setObservacao(request.observacao());
        return AutorizacaoRetiradaResponse.from(autorizacaoRepository.save(autorizacao));
    }

    /** Aprovacao nao exige motivo: o ato de aprovar ja e' a decisao registrada. */
    @Transactional
    public AutorizacaoRetiradaResponse aprovar(UUID id, String motivo, String ip) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        AutorizacaoRetirada autorizacao = carregar(id, tenantId);
        if (autorizacao.getStatus() != StatusAutorizacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente autorização pendente pode ser aprovada");
        }
        // Aprovar uma temporaria ja vencida so cria falsa sensacao de liberacao.
        if (autorizacao.getVigenciaFim() != null && autorizacao.getVigenciaFim().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Autorização já está fora da vigência; ajuste a vigência antes de aprovar");
        }
        return transicionar(autorizacao, AcaoAutorizacao.APROVACAO, StatusAutorizacao.ATIVA, motivo, ip, true);
    }

    /** Suspender EXIGE motivo: e' uma restricao de acesso e precisa de rastro. */
    @Transactional
    public AutorizacaoRetiradaResponse suspender(UUID id, String motivo, String ip) {
        exigirMotivo(motivo);
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        AutorizacaoRetirada autorizacao = carregar(id, tenantId);
        if (autorizacao.getStatus() != StatusAutorizacao.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente autorização ativa pode ser suspensa");
        }
        return transicionar(autorizacao, AcaoAutorizacao.SUSPENSAO, StatusAutorizacao.SUSPENSA, motivo, ip, false);
    }

    @Transactional
    public AutorizacaoRetiradaResponse reativar(UUID id, String motivo, String ip) {
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        AutorizacaoRetirada autorizacao = carregar(id, tenantId);
        if (autorizacao.getStatus() != StatusAutorizacao.SUSPENSA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente autorização suspensa pode ser reativada");
        }
        if (autorizacao.getVigenciaFim() != null && autorizacao.getVigenciaFim().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Autorização já está fora da vigência; ajuste a vigência antes de reativar");
        }
        return transicionar(autorizacao, AcaoAutorizacao.REATIVACAO, StatusAutorizacao.ATIVA, motivo, ip, true);
    }

    /**
     * Revogar EXIGE motivo e e' terminal. Nao voltamos de REVOGADA: se a
     * escola mudar de ideia, cria uma autorizacao nova, e a trilha continua
     * mostrando que houve uma revogacao e por que.
     */
    @Transactional
    public AutorizacaoRetiradaResponse revogar(UUID id, String motivo, String ip) {
        exigirMotivo(motivo);
        UUID tenantId = ContextoAtual.tenantObrigatorio();
        AutorizacaoRetirada autorizacao = carregar(id, tenantId);
        if (autorizacao.getStatus() == StatusAutorizacao.REVOGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Autorização já está revogada");
        }
        return transicionar(autorizacao, AcaoAutorizacao.REVOGACAO, StatusAutorizacao.REVOGADA, motivo, ip, false);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private AutorizacaoRetiradaResponse transicionar(AutorizacaoRetirada autorizacao,
                                                     AcaoAutorizacao acao,
                                                     StatusAutorizacao novoStatus,
                                                     String motivo,
                                                     String ip,
                                                     boolean marcarAprovacao) {
        StatusAutorizacao anterior = autorizacao.getStatus();
        autorizacao.setStatus(novoStatus);
        if (marcarAprovacao) {
            autorizacao.setAprovadoPorUserId(ContextoAtual.userIdAtual());
            autorizacao.setAprovadoEm(Instant.now());
        }
        if (motivo != null && !motivo.isBlank()) {
            autorizacao.setMotivo(motivo.trim());
        }
        AutorizacaoRetirada salva = autorizacaoRepository.save(autorizacao);
        gravarHistorico(salva, acao, anterior, novoStatus, ContextoAtual.userIdAtual(), motivo, ip);
        return AutorizacaoRetiradaResponse.from(salva);
    }

    private void gravarHistorico(AutorizacaoRetirada autorizacao,
                                 AcaoAutorizacao acao,
                                 StatusAutorizacao anterior,
                                 StatusAutorizacao novo,
                                 UUID userId,
                                 String motivo,
                                 String ip) {
        historicoRepository.save(new AutorizacaoHistorico(
                autorizacao.getTenantId(), autorizacao.getId(), acao, anterior, novo, userId, motivo, ip));
    }

    private void aplicarJanela(AutorizacaoRetirada autorizacao, AutorizacaoRetiradaRequest request) {
        boolean permanente = Boolean.TRUE.equals(request.permanente());

        // Sem esta guarda, "a tia busca nesta sexta" vira autorizacao eterna.
        if (!permanente && request.vigenciaFim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Autorização temporária exige data de fim de vigência");
        }
        if (request.vigenciaInicio() != null && request.vigenciaFim() != null
                && request.vigenciaFim().isBefore(request.vigenciaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fim de vigência não pode ser anterior ao início");
        }
        if (request.horaInicio() != null && request.horaFim() != null
                && request.horaFim().isBefore(request.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hora fim não pode ser anterior à hora início");
        }

        autorizacao.setPermanente(permanente);
        autorizacao.setVigenciaInicio(request.vigenciaInicio());
        autorizacao.setVigenciaFim(request.vigenciaFim());
        autorizacao.setDiasSemana(normalizarDiasSemana(request.diasSemana()));
        autorizacao.setHoraInicio(request.horaInicio());
        autorizacao.setHoraFim(request.horaFim());
    }

    /**
     * Aceita "1,3,5" no padrao ISO (1=segunda ... 7=domingo) e devolve
     * ordenado e sem repeticao. Qualquer token fora de 1..7 e' erro de
     * entrada: silenciar um "0" ou "8" produziria uma regra de dias que a
     * escola acha que existe e que nunca casa.
     */
    private String normalizarDiasSemana(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        List<Integer> dias = new java.util.ArrayList<>();
        for (String parte : csv.split(",")) {
            String token = parte.trim();
            if (token.isEmpty()) {
                continue;
            }
            int dia;
            try {
                dia = Integer.parseInt(token);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dias da semana devem ser números de 1 a 7 separados por vírgula");
            }
            if (dia < 1 || dia > 7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dias da semana devem estar entre 1 (segunda) e 7 (domingo)");
            }
            if (!dias.contains(dia)) {
                dias.add(dia);
            }
        }
        if (dias.isEmpty()) {
            return null;
        }
        dias.sort(Comparator.naturalOrder());
        return dias.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null);
    }

    private void exigirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo é obrigatório");
        }
    }

    private AutorizacaoRetirada carregar(UUID id, UUID tenantId) {
        return autorizacaoRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autorização não encontrada"));
    }
}
