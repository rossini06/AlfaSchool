package br.com.alfaschool.backend.application.financeiro;

import br.com.alfaschool.backend.application.financeiro.dto.*;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financeiro")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    // ─── Planos ───────────────────────────────────────────────────────────────

    @GetMapping("/planos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PlanoFinanceiroResponse>>> listPlanos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.of(200, "Planos listados",
                financeiroService.listPlanos(PageRequest.of(page, size, Sort.by("nome").ascending()))));
    }

    @GetMapping("/planos/ativos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PlanoFinanceiroResponse>>> listPlanosAtivos() {
        return ResponseEntity.ok(ApiResponse.of(200, "Planos ativos", financeiroService.listPlanosAtivos()));
    }

    @PostMapping("/planos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PlanoFinanceiroResponse>> createPlano(
            @Valid @RequestBody PlanoFinanceiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Plano criado", financeiroService.createPlano(request)));
    }

    @PutMapping("/planos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PlanoFinanceiroResponse>> updatePlano(@PathVariable UUID id,
            @Valid @RequestBody PlanoFinanceiroRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Plano atualizado", financeiroService.updatePlano(id, request)));
    }

    @DeleteMapping("/planos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePlano(@PathVariable UUID id) {
        financeiroService.deletePlano(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Plano removido", null));
    }

    // ─── Contratos ────────────────────────────────────────────────────────────

    @GetMapping("/contratos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ContratoResponse>>> listContratos(
            @RequestParam(required = false) UUID alunoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (alunoId != null) {
            List<ContratoResponse> items = financeiroService.listContratosByAluno(alunoId);
            return ResponseEntity.ok(ApiResponse.of(200, "Contratos do aluno",
                    new org.springframework.data.domain.PageImpl<>(items)));
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Contratos",
                financeiroService.listContratos(PageRequest.of(page, size, Sort.by("dataInicio").descending()))));
    }

    @PostMapping("/contratos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContratoResponse>> createContrato(
            @Valid @RequestBody ContratoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Contrato criado", financeiroService.createContrato(request)));
    }

    @PatchMapping("/contratos/{id}/encerrar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContratoResponse>> encerrarContrato(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Contrato encerrado", financeiroService.encerrarContrato(id)));
    }

    // ─── Cobranças ────────────────────────────────────────────────────────────

    @GetMapping("/cobrancas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<CobrancaResponse>>> listCobrancas(
            @RequestParam(required = false) UUID alunoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (alunoId != null) {
            List<CobrancaResponse> items = financeiroService.listCobrancasByAluno(alunoId);
            return ResponseEntity.ok(ApiResponse.of(200, "Cobranças do aluno",
                    new org.springframework.data.domain.PageImpl<>(items)));
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Cobranças",
                financeiroService.listCobrancas(PageRequest.of(page, size, Sort.by("vencimento").ascending()))));
    }

    @PatchMapping("/cobrancas/{id}/pagar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CobrancaResponse>> registrarPagamento(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPagamento) {
        return ResponseEntity.ok(ApiResponse.of(200, "Pagamento registrado",
                financeiroService.registrarPagamento(id, dataPagamento)));
    }
}
