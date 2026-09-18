package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Vincula o acesso de um responsavel ao cadastro dele.
 *
 * Existe porque o casamento por e-mail nao e' garantia: dois responsaveis
 * que dividem a mesma caixa ficam ambiguos, e o portal passa a recusar os
 * dois (V44). Quem desfaz o empate e' a secretaria, aqui.
 */
@RestController
@RequestMapping("/api/v1/access/portal/vinculos")
public class PortalVinculoController {

    private final PortalVinculoService service;

    public PortalVinculoController(PortalVinculoService service) {
        this.service = service;
    }

    public record VincularRequest(
            @NotNull(message = "responsavelId é obrigatório.") UUID responsavelId,
            @NotNull(message = "userId é obrigatório.") UUID userId) {
    }

    /** Responsaveis do tenant que ainda nao tem acesso vinculado. */
    @GetMapping("/pendentes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> pendentes() {
        return ResponseEntity.ok(ApiResponse.of(200, "Responsáveis sem acesso vinculado",
                service.pendentes()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> vincular(@Valid @RequestBody VincularRequest req) {
        service.vincular(req.responsavelId(), req.userId());
        return ResponseEntity.ok(ApiResponse.of(200, "Acesso vinculado ao responsável", null));
    }

    @DeleteMapping("/{responsavelId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> desvincular(@PathVariable UUID responsavelId) {
        service.desvincular(responsavelId);
        return ResponseEntity.ok(ApiResponse.of(200, "Acesso desvinculado", null));
    }
}
