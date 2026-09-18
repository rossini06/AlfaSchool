package br.com.alfaschool.backend.application.curso;

import br.com.alfaschool.backend.application.curso.dto.CursoRequest;
import br.com.alfaschool.backend.application.curso.dto.CursoResponse;
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
@RequestMapping("/api/v1/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_TURMAS_VER')")
    public ResponseEntity<ApiResponse<Page<CursoResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Cursos listados com sucesso", cursoService.list(q, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_TURMAS_VER')")
    public ResponseEntity<ApiResponse<CursoResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Curso encontrado", cursoService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<CursoResponse>> create(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Curso criado com sucesso", cursoService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<CursoResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Curso atualizado com sucesso", cursoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        cursoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Curso removido com sucesso", null));
    }
}
