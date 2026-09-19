package br.com.alfaschool.backend.application.aluno;

import br.com.alfaschool.backend.application.aluno.dto.AlunoRequest;
import br.com.alfaschool.backend.application.aluno.dto.AlunoResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alunos")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ALUNOS_VER')")
    public ResponseEntity<ApiResponse<Page<AlunoResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("nome").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Alunos listados com sucesso", alunoService.list(q, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ALUNOS_VER')")
    public ResponseEntity<ApiResponse<AlunoResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Aluno encontrado", alunoService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ALUNOS_GERIR')")
    public ResponseEntity<ApiResponse<AlunoResponse>> create(@Valid @RequestBody AlunoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Aluno criado com sucesso", alunoService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ALUNOS_GERIR')")
    public ResponseEntity<ApiResponse<AlunoResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody AlunoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Aluno atualizado com sucesso", alunoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ALUNOS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        alunoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Aluno removido com sucesso", null));
    }
}
