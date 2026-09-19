package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoHistoricoResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoRetiradaRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.AutorizacaoRetiradaResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.MotivoOpcionalRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.MotivoRequest;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.alfaschool.backend.shared.web.Paginacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/autorizacoes")
public class AutorizacaoRetiradaController {

    private final AutorizacaoRetiradaService autorizacaoRetiradaService;

    public AutorizacaoRetiradaController(AutorizacaoRetiradaService autorizacaoRetiradaService) {
        this.autorizacaoRetiradaService = autorizacaoRetiradaService;
    }

    /**
     * Listagem geral. Nao existia: so' havia busca por aluno e por pessoa,
     * entao GET /access/autorizacoes respondia 405 e tanto a tabela
     * principal quanto a fila de aprovacao ficavam permanentemente vazias.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<Page<AutorizacaoRetiradaResponse>>> listar(
            @RequestParam(required = false) UUID alunoId,
            @RequestParam(required = false) StatusAutorizacao status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size);
        return ResponseEntity.ok(ApiResponse.of(200, "Autorizações listadas com sucesso",
                autorizacaoRetiradaService.listar(alunoId, status, q, pageable)));
    }

    @GetMapping("/aluno/{alunoId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<List<AutorizacaoRetiradaResponse>>> listarPorAluno(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorizações do aluno listadas com sucesso",
                autorizacaoRetiradaService.listarPorAluno(alunoId)));
    }

    @GetMapping("/pessoa/{pessoaAutorizadaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<List<AutorizacaoRetiradaResponse>>> listarPorPessoa(
            @PathVariable UUID pessoaAutorizadaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorizações da pessoa listadas com sucesso",
                autorizacaoRetiradaService.listarPorPessoa(pessoaAutorizadaId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorização encontrada",
                autorizacaoRetiradaService.findById(id)));
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<List<AutorizacaoHistoricoResponse>>> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Histórico da autorização listado com sucesso",
                autorizacaoRetiradaService.historico(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_GERIR')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> criar(
            @Valid @RequestBody AutorizacaoRetiradaRequest request, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Autorização criada com sucesso",
                        autorizacaoRetiradaService.criar(request, ContextoAtual.ipDe(http))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_GERIR')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> atualizar(
            @PathVariable UUID id, @Valid @RequestBody AutorizacaoRetiradaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorização atualizada com sucesso",
                autorizacaoRetiradaService.atualizar(id, request)));
    }

    /**
     * Aprovar libera de verdade a retirada de uma crianca, entao nao basta
     * ter o modulo: exigimos perfil da escola com poder de decisao.
     */
    @PostMapping("/{id}/aprovar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_APROVAR')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> aprovar(
            @PathVariable UUID id,
            @RequestBody(required = false) MotivoOpcionalRequest request,
            HttpServletRequest http) {
        String motivo = request != null ? request.motivo() : null;
        return ResponseEntity.ok(ApiResponse.of(200, "Autorização aprovada com sucesso",
                autorizacaoRetiradaService.aprovar(id, motivo, ContextoAtual.ipDe(http))));
    }

    @PostMapping("/{id}/suspender")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_APROVAR')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> suspender(
            @PathVariable UUID id, @Valid @RequestBody MotivoRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorização suspensa com sucesso",
                autorizacaoRetiradaService.suspender(id, request.motivo(), ContextoAtual.ipDe(http))));
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_APROVAR')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> reativar(
            @PathVariable UUID id,
            @RequestBody(required = false) MotivoOpcionalRequest request,
            HttpServletRequest http) {
        String motivo = request != null ? request.motivo() : null;
        return ResponseEntity.ok(ApiResponse.of(200, "Autorização reativada com sucesso",
                autorizacaoRetiradaService.reativar(id, motivo, ContextoAtual.ipDe(http))));
    }

    @PostMapping("/{id}/revogar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_APROVAR')")
    public ResponseEntity<ApiResponse<AutorizacaoRetiradaResponse>> revogar(
            @PathVariable UUID id, @Valid @RequestBody MotivoRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorização revogada com sucesso",
                autorizacaoRetiradaService.revogar(id, request.motivo(), ContextoAtual.ipDe(http))));
    }
}
