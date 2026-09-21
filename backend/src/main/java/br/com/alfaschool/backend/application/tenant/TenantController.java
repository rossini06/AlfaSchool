package br.com.alfaschool.backend.application.tenant;

import br.com.alfaschool.backend.application.modulo.dto.ModuloContratacaoResponse;
import br.com.alfaschool.backend.application.tenant.dto.TenantCreateRequest;
import br.com.alfaschool.backend.application.tenant.dto.TenantModulosRequest;
import br.com.alfaschool.backend.application.tenant.dto.TenantResponse;
import br.com.alfaschool.backend.application.tenant.dto.TenantUpdateRequest;
import br.com.alfaschool.backend.application.tenant.dto.TenantStatusRequest;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Redes de ensino listadas com sucesso", tenantService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Rede de ensino encontrada", tenantService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> criar(@Valid @RequestBody TenantCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Rede de ensino criada com sucesso", tenantService.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> atualizar(@PathVariable UUID id,
                                                                  @Valid @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Rede de ensino atualizada com sucesso",
                tenantService.atualizar(id, request)));
    }

    @GetMapping("/{id}/modulos")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ModuloContratacaoResponse>>> modulos(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Módulos da rede", tenantService.listarModulos(id)));
    }

    @PutMapping("/{id}/modulos")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ModuloContratacaoResponse>>> definirModulos(
            @PathVariable UUID id, @Valid @RequestBody TenantModulosRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Módulos atualizados com sucesso",
                tenantService.definirModulos(id, request.modulos())));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> changeStatus(@PathVariable UUID id,
                                                                     @Valid @RequestBody TenantStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Status atualizado com sucesso",
                tenantService.changeStatus(id, request.status())));
    }
}
