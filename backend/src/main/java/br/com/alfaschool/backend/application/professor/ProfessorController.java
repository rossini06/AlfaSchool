package br.com.alfaschool.backend.application.professor;

import br.com.alfaschool.backend.application.professor.dto.ProfessorRequest;
import br.com.alfaschool.backend.application.professor.dto.ProfessorResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/professores")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PROFESSORES_VER')")
    public ResponseEntity<ApiResponse<Page<ProfessorResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.of(200, "Professores listados",
                professorService.list(q, PageRequest.of(page, size, Sort.by("nome").ascending()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PROFESSORES_VER')")
    public ResponseEntity<ApiResponse<ProfessorResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Professor encontrado", professorService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PROFESSORES_GERIR')")
    public ResponseEntity<ApiResponse<ProfessorResponse>> create(@Valid @RequestBody ProfessorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Professor criado com sucesso", professorService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PROFESSORES_GERIR')")
    public ResponseEntity<ApiResponse<ProfessorResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody ProfessorRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Professor atualizado", professorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PROFESSORES_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        professorService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Professor removido", null));
    }
}
