package br.com.alfaschool.backend.application.diario;

import br.com.alfaschool.backend.application.diario.dto.ConteudoMinistradoRequest;
import br.com.alfaschool.backend.application.diario.dto.ConteudoMinistradoResponse;
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
@RequestMapping("/api/v1/conteudos-ministrados")
public class ConteudoMinistradoController {

    private final ConteudoMinistradoService conteudoMinistradoService;

    public ConteudoMinistradoController(ConteudoMinistradoService conteudoMinistradoService) {
        this.conteudoMinistradoService = conteudoMinistradoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ConteudoMinistradoResponse>>> list(
            @RequestParam UUID turmaId,
            @RequestParam UUID disciplinaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("data").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Conteúdos ministrados",
                conteudoMinistradoService.list(turmaId, disciplinaId, pageable)));
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ConteudoMinistradoResponse>>> listAll(
            @RequestParam UUID turmaId,
            @RequestParam UUID disciplinaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Todos os conteúdos ministrados",
                conteudoMinistradoService.listAll(turmaId, disciplinaId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConteudoMinistradoResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Conteúdo ministrado",
                conteudoMinistradoService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConteudoMinistradoResponse>> create(
            @Valid @RequestBody ConteudoMinistradoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Conteúdo registrado",
                        conteudoMinistradoService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConteudoMinistradoResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ConteudoMinistradoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Conteúdo atualizado",
                conteudoMinistradoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        conteudoMinistradoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Conteúdo removido", null));
    }
}
