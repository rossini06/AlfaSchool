package br.com.alfaschool.backend.application.nota;

import br.com.alfaschool.backend.application.nota.dto.NotaRequest;
import br.com.alfaschool.backend.application.nota.dto.NotaResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notas")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    @GetMapping("/aluno/{alunoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotaResponse>>> listByAluno(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Notas do aluno", notaService.listByAluno(alunoId)));
    }

    @GetMapping("/matricula/{matriculaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotaResponse>>> listByMatricula(@PathVariable UUID matriculaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Notas da matrícula", notaService.listByMatricula(matriculaId)));
    }

    @GetMapping("/avaliacao/{avaliacaoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotaResponse>>> listByAvaliacao(@PathVariable UUID avaliacaoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Notas da avaliação", notaService.listByAvaliacao(avaliacaoId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotaResponse>> lancar(@Valid @RequestBody NotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Nota lançada", notaService.lancar(request)));
    }

    @PatchMapping("/{id}/recuperacao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotaResponse>> lancarRecuperacao(
            @PathVariable UUID id,
            @RequestParam BigDecimal notaRecuperacao) {
        return ResponseEntity.ok(ApiResponse.of(200, "Nota de recuperação lançada",
                notaService.lancarRecuperacao(id, notaRecuperacao)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        notaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Nota removida", null));
    }
}
