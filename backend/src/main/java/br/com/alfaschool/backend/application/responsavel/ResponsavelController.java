package br.com.alfaschool.backend.application.responsavel;

import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelRequest;
import br.com.alfaschool.backend.application.responsavel.dto.ResponsavelResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/responsaveis")
public class ResponsavelController {

    private final ResponsavelService responsavelService;

    public ResponsavelController(ResponsavelService responsavelService) {
        this.responsavelService = responsavelService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ResponsavelResponse>>> listByAluno(@RequestParam UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Responsáveis listados", responsavelService.listByAluno(alunoId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResponsavelResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Responsável encontrado", responsavelService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResponsavelResponse>> create(@Valid @RequestBody ResponsavelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Responsável criado", responsavelService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResponsavelResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody ResponsavelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Responsável atualizado", responsavelService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        responsavelService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Responsável removido", null));
    }
}
