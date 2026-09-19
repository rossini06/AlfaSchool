package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.application.access.notificacao.dto.EnvioResponse;
import br.com.alfaschool.backend.application.access.portal.dto.PortalAlunoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalAutorizacaoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalPermanenciaDia;
import br.com.alfaschool.backend.application.access.portal.dto.PortalResumoDia;
import br.com.alfaschool.backend.application.access.portal.dto.PortalSolicitacaoAutorizacaoRequest;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Portal da familia.
 *
 * <p>Repare no que NAO existe aqui: nenhum endpoint recebe id de responsavel.
 * O escopo vem do token, sempre. {@code /aluno/{id}} so' aceita id que ja
 * esteja na lista derivada do usuario autenticado; qualquer outro devolve 404.
 */
@RestController
@RequestMapping("/api/v1/access/portal")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/meus-alunos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_PORTAL_ACESSAR')")
    public ResponseEntity<ApiResponse<List<PortalAlunoResumo>>> meusAlunos() {
        return ResponseEntity.ok(ApiResponse.of(200, "Alunos listados com sucesso", portalService.meusAlunos()));
    }

    @GetMapping("/aluno/{alunoId}/hoje")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_PORTAL_ACESSAR')")
    public ResponseEntity<ApiResponse<PortalResumoDia>> hoje(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Resumo do dia carregado com sucesso",
                portalService.hoje(alunoId)));
    }

    @GetMapping("/aluno/{alunoId}/historico")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_PORTAL_ACESSAR')")
    public ResponseEntity<ApiResponse<List<PortalPermanenciaDia>>> historico(
            @PathVariable UUID alunoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(ApiResponse.of(200, "Histórico carregado com sucesso",
                portalService.historico(alunoId, inicio, fim)));
    }

    @GetMapping("/aluno/{alunoId}/autorizacoes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_PORTAL_ACESSAR')")
    public ResponseEntity<ApiResponse<List<PortalAutorizacaoResumo>>> autorizacoes(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Autorizações listadas com sucesso",
                portalService.autorizacoes(alunoId)));
    }

    /**
     * Solicita a inclusao de uma pessoa autorizada. Devolve 202: foi ACEITO
     * para analise, nao concedido. A escola aprova.
     */
    @PostMapping("/aluno/{alunoId}/autorizacoes/solicitar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_PORTAL_ACESSAR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> solicitarAutorizacao(
            @PathVariable UUID alunoId,
            @Valid @RequestBody PortalSolicitacaoAutorizacaoRequest request) {
        UUID id = portalService.solicitarAutorizacao(alunoId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(202,
                "Solicitação registrada e pendente de aprovação da escola",
                Map.of("solicitacaoId", id, "status", "PENDENTE", "origem", "PORTAL")));
    }

    @GetMapping("/notificacoes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_PORTAL_ACESSAR')")
    public ResponseEntity<ApiResponse<Page<EnvioResponse>>> notificacoes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size);
        return ResponseEntity.ok(ApiResponse.of(200, "Notificações listadas com sucesso",
                portalService.minhasNotificacoes(pageable)));
    }
}
