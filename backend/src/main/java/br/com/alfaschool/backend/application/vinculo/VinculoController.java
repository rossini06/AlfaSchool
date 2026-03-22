package br.com.alfaschool.backend.application.vinculo;

import br.com.alfaschool.backend.application.vinculo.dto.VinculoRequest;
import br.com.alfaschool.backend.application.vinculo.dto.VinculoResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vinculos")
public class VinculoController {

    private final VinculoService vinculoService;

    public VinculoController(VinculoService vinculoService) {
        this.vinculoService = vinculoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<VinculoResponse>>> list(
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(required = false) UUID professorId) {
        if (turmaId != null) {
            return ResponseEntity.ok(ApiResponse.of(200, "Vínculos por turma", vinculoService.listByTurma(turmaId)));
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculos por professor", vinculoService.listByProfessor(professorId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VinculoResponse>> create(@Valid @RequestBody VinculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Vínculo criado", vinculoService.create(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        vinculoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculo removido", null));
    }
}
