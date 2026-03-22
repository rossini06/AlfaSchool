package br.com.alfaschool.backend.application.turma;

import br.com.alfaschool.backend.application.turma.dto.TurmaRequest;
import br.com.alfaschool.backend.application.turma.dto.TurmaResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/turmas")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID cursoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (cursoId != null) {
            List<TurmaResponse> result = turmaService.listByCurso(cursoId);
            return ResponseEntity.ok(ApiResponse.of(200, "Turmas listadas com sucesso", result));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<TurmaResponse> result = turmaService.list(q, pageable);
        return ResponseEntity.ok(ApiResponse.of(200, "Turmas listadas com sucesso", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TurmaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Turma encontrada", turmaService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TurmaResponse>> create(@Valid @RequestBody TurmaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Turma criada com sucesso", turmaService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TurmaResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody TurmaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Turma atualizada com sucesso", turmaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        turmaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Turma removida com sucesso", null));
    }
}
