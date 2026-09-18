package br.com.alfaschool.backend.application.unit;

import br.com.alfaschool.backend.application.unit.dto.UnitRequest;
import br.com.alfaschool.backend.application.unit.dto.UnitResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ESCOLA_VER')")
    public ResponseEntity<ApiResponse<Page<UnitResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Escolas listadas com sucesso", unitService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ESCOLA_VER')")
    public ResponseEntity<ApiResponse<UnitResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Escola encontrada", unitService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ESCOLA_GERIR')")
    public ResponseEntity<ApiResponse<UnitResponse>> create(@Valid @RequestBody UnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Escola criada com sucesso", unitService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ESCOLA_GERIR')")
    public ResponseEntity<ApiResponse<UnitResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UnitRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Escola atualizada com sucesso", unitService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ESCOLA_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        unitService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Escola removida com sucesso", null));
    }
}
