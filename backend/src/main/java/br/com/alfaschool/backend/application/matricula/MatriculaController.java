package br.com.alfaschool.backend.application.matricula;

import br.com.alfaschool.backend.application.matricula.dto.MatriculaRequest;
import br.com.alfaschool.backend.application.matricula.dto.MatriculaResponse;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<MatriculaResponse>>> list(
            @RequestParam(required = false) UUID alunoId,
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dataMatricula").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Matrículas listadas com sucesso",
                matriculaService.list(alunoId, turmaId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula encontrada", matriculaService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> create(@Valid @RequestBody MatriculaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Matrícula criada com sucesso", matriculaService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> update(@PathVariable UUID id,
                                                                  @Valid @RequestBody MatriculaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula atualizada com sucesso", matriculaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        matriculaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula removida com sucesso", null));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula cancelada com sucesso", matriculaService.cancelar(id)));
    }

    @PostMapping("/{id}/trancar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> trancar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula trancada com sucesso", matriculaService.trancar(id)));
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> reativar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula reativada com sucesso", matriculaService.reativar(id)));
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> concluir(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matrícula concluída com sucesso", matriculaService.concluir(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MatriculaResponse>> updateStatus(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(ApiResponse.of(200, "Status atualizado com sucesso",
                matriculaService.updateStatus(id, status)));
    }
}
