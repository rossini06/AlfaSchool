package br.com.alfaschool.backend.application.avaliacao;

import br.com.alfaschool.backend.application.avaliacao.dto.AvaliacaoRequest;
import br.com.alfaschool.backend.application.avaliacao.dto.AvaliacaoResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (turmaId != null && disciplinaId != null) {
            return ResponseEntity.ok(ApiResponse.of(200, "Avaliações",
                    org.springframework.data.domain.Page.empty(PageRequest.of(page, size))));
        }
        if (turmaId != null) {
            List<AvaliacaoResponse> items = avaliacaoService.listByTurma(turmaId);
            return ResponseEntity.ok(ApiResponse.of(200, "Avaliações da turma",
                    new org.springframework.data.domain.PageImpl<>(items)));
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Avaliações",
                avaliacaoService.list(PageRequest.of(page, size, Sort.by("dataAvaliacao").descending()))));
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
