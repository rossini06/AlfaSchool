package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.dto.EntregaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.FilaFiltro;
import br.com.alfaschool.backend.application.access.retirada.dto.MotivoRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RegistrarSaidaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaDetalheResponse;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaManualRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaResponse;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/retiradas")
public class RetiradaController {

    private final RetiradaService retiradaService;
    private final RetiradaConsultaService consultaService;

    public RetiradaController(RetiradaService retiradaService, RetiradaConsultaService consultaService) {
        this.retiradaService = retiradaService;
        this.consultaService = consultaService;
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    @GetMapping("/fila")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<List<RetiradaFilaItem>>> fila(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(required = false) UUID salaId,
            @RequestParam(required = false) UUID portariaId,
            @RequestParam(required = false) List<StatusRetirada> status,
            @RequestParam(required = false) Integer esperandoHaMinutos) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        FilaFiltro filtro = new FilaFiltro(unitId, turmaId, salaId, portariaId, status, esperandoHaMinutos);
        return ResponseEntity.ok(ApiResponse.of(200, "Fila de retirada carregada com sucesso",
                consultaService.fila(tenantId, filtro)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<RetiradaDetalheResponse>> detalhe(@PathVariable UUID id) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return ResponseEntity.ok(ApiResponse.of(200, "Retirada encontrada",
                consultaService.detalhe(tenantId, id)));
    }

    @GetMapping("/historico")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<List<RetiradaFilaItem>>> historico(
            @RequestParam(required = false) UUID alunoId,
            @RequestParam(required = false) Instant inicio,
            @RequestParam(required = false) Instant fim) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return ResponseEntity.ok(ApiResponse.of(200, "Historico de retiradas carregado com sucesso",
                consultaService.historicoDoAluno(tenantId, alunoId, inicio, fim)));
    }

    // ---------------------------------------------------------------
    // Transicoes
    // ---------------------------------------------------------------

    @PostMapping("/manual")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_MANUAL')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> abrirManual(
            @Valid @RequestBody RetiradaManualRequest request,
            HttpServletRequest http) {
        RetiradaResponse resposta = RetiradaResponse.from(
                retiradaService.abrirManual(request, ContextoAcesso.ipDaRequisicao(http)));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Retirada manual aberta com sucesso", resposta));
    }

    @PostMapping("/{id}/preparar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_OPERAR')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> preparar(@PathVariable UUID id, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Retirada em preparo",
                RetiradaResponse.from(retiradaService.preparar(id, ContextoAcesso.ipDaRequisicao(http)))));
    }

    @PostMapping("/{id}/pronto")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_OPERAR')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> pronto(@PathVariable UUID id, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Aluno pronto para retirada",
                RetiradaResponse.from(retiradaService.pronto(id, ContextoAcesso.ipDaRequisicao(http)))));
    }

    /**
     * Confirma a entrega da crianca. Exige colaborador autenticado.
     *
     * NAO encerra a permanencia: entregar nao e' sair. Ver
     * POST /{id}/registrar-saida.
     */
    @PostMapping("/{id}/entregar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_ENTREGAR')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> entregar(@PathVariable UUID id,
                                                                  @RequestBody(required = false) EntregaRequest request,
                                                                  HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Entrega confirmada com sucesso",
                RetiradaResponse.from(retiradaService.entregar(id, request, ContextoAcesso.ipDaRequisicao(http)))));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_OPERAR')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> cancelar(@PathVariable UUID id,
                                                                   @Valid @RequestBody MotivoRequest request,
                                                                   HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Retirada cancelada",
                RetiradaResponse.from(retiradaService.cancelar(id, request.motivo(), ContextoAcesso.ipDaRequisicao(http)))));
    }

    @PostMapping("/{id}/negar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_OPERAR')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> negar(@PathVariable UUID id,
                                                                @Valid @RequestBody MotivoRequest request,
                                                                HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Retirada negada",
                RetiradaResponse.from(retiradaService.negar(id, request.motivo(), ContextoAcesso.ipDaRequisicao(http)))));
    }

    /**
     * Saida efetiva registrada a mao.
     *
     * USE SOMENTE em escola sem catraca de saida. Onde existe catraca, a
     * saida chega pelo evento de leitura e este endpoint criaria um segundo
     * fechamento de permanencia para o mesmo aluno.
     */
    @PostMapping("/{id}/registrar-saida")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RETIRADA_OPERAR')")
    public ResponseEntity<ApiResponse<RetiradaResponse>> registrarSaida(
            @PathVariable UUID id,
            @RequestBody(required = false) RegistrarSaidaRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.of(200, "Saida registrada com sucesso",
                RetiradaResponse.from(retiradaService.registrarSaida(id, request, ContextoAcesso.ipDaRequisicao(http)))));
    }
}
