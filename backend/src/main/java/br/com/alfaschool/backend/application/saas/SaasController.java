package br.com.alfaschool.backend.application.saas;

import br.com.alfaschool.backend.application.saas.dto.SaasMetricasResponse;
import br.com.alfaschool.backend.application.saas.dto.SaasPlanRequest;
import br.com.alfaschool.backend.application.saas.dto.SaasPlanResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saas")
public class SaasController {

    private final SaasService saasService;

    public SaasController(SaasService saasService) {
        this.saasService = saasService;
    }

    /**
     * Catalogo comercial da Alfa: preco, limite e o que cada plano inclui.
     * Nao e' assunto de escola. Estava aberto a qualquer usuario logado de
     * qualquer tenant — um porteiro lia a tabela de precos inteira.
     */
    @GetMapping("/plans")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SaasPlanResponse>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.of(200, "Planos listados com sucesso", saasService.listPlans()));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SaasPlanResponse>> createPlan(@Valid @RequestBody SaasPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Plano criado com sucesso", saasService.createPlan(request)));
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SaasPlanResponse>> updatePlan(@PathVariable UUID id,
                                                                     @Valid @RequestBody SaasPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Plano atualizado com sucesso", saasService.updatePlan(id, request)));
    }

    @DeleteMapping("/plans/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable UUID id) {
        saasService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Plano removido com sucesso", null));
    }

    @GetMapping("/metricas")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SaasMetricasResponse>> metricas() {
        return ResponseEntity.ok(ApiResponse.of(200, "Métricas carregadas com sucesso", saasService.metricas()));
    }
}
