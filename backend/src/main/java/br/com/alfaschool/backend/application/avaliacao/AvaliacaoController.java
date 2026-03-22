package br.com.alfaschool.backend.application.avaliacao;

import br.com.alfaschool.backend.application.avaliacao.dto.AvaliacaoRequest;
import br.com.alfaschool.backend.application.avaliacao.dto.AvaliacaoResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<AvaliacaoResponse>>> list(
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(required = false) UUID disciplinaId,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // If any filter is active, use the search method
        if (turmaId != null || disciplinaId != null || periodo != null || status != null) {
            if (turmaId != null && disciplinaId != null && periodo == null && status == null) {
                // Exact turma+disciplina match (used by NotasPage)
                List<AvaliacaoResponse> items = avaliacaoService.listByTurmaAndDisciplina(turmaId, disciplinaId);
                return ResponseEntity.ok(ApiResponse.of(200, "Avaliações",
                        new PageImpl<>(items, PageRequest.of(0, Math.max(items.size(), 1)), items.size())));
            }
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("dataAvaliacao").descending());
            return ResponseEntity.ok(ApiResponse.of(200, "Avaliações",
                    avaliacaoService.search(turmaId, disciplinaId, periodo, status, pageRequest)));
        }

        return ResponseEntity.ok(ApiResponse.of(200, "Avaliações",
                avaliacaoService.list(PageRequest.of(page, size, Sort.by("dataAvaliacao").descending()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AvaliacaoResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Avaliação", avaliacaoService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AvaliacaoResponse>> create(@Valid @RequestBody AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Avaliação criada", avaliacaoService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AvaliacaoResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody AvaliacaoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Avaliação atualizada", avaliacaoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        avaliacaoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Avaliação removida", null));
    }
}
