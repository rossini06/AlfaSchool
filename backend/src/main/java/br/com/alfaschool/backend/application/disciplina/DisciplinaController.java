package br.com.alfaschool.backend.application.disciplina;

import br.com.alfaschool.backend.application.disciplina.dto.DisciplinaRequest;
import br.com.alfaschool.backend.application.disciplina.dto.DisciplinaResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disciplinas")
public class DisciplinaController {

    private final DisciplinaService disciplinaService;

    public DisciplinaController(DisciplinaService disciplinaService) {
        this.disciplinaService = disciplinaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_TURMAS_VER')")
    public ResponseEntity<ApiResponse<Page<DisciplinaResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.of(200, "Disciplinas listadas",
                disciplinaService.list(q, Paginacao.de(page, size, Sort.by("nome").ascending()))));
    }

    @GetMapping("/ativas")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_TURMAS_VER')")
    public ResponseEntity<ApiResponse<List<DisciplinaResponse>>> listAtivas() {
        return ResponseEntity.ok(ApiResponse.of(200, "Disciplinas ativas", disciplinaService.listAtivas()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_TURMAS_VER')")
    public ResponseEntity<ApiResponse<DisciplinaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Disciplina encontrada", disciplinaService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<DisciplinaResponse>> create(@Valid @RequestBody DisciplinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Disciplina criada", disciplinaService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<DisciplinaResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody DisciplinaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Disciplina atualizada", disciplinaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        disciplinaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Disciplina removida", null));
    }
}
