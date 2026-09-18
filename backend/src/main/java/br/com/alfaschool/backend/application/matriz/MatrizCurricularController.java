package br.com.alfaschool.backend.application.matriz;

import br.com.alfaschool.backend.application.matriz.dto.MatrizRequest;
import br.com.alfaschool.backend.application.matriz.dto.MatrizResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matriz-curricular")
public class MatrizCurricularController {

    private final MatrizCurricularService matrizService;

    public MatrizCurricularController(MatrizCurricularService matrizService) {
        this.matrizService = matrizService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_TURMAS_VER')")
    public ResponseEntity<ApiResponse<List<MatrizResponse>>> listByCurso(@RequestParam UUID cursoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Matriz curricular", matrizService.listByCurso(cursoId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<MatrizResponse>> create(@Valid @RequestBody MatrizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Disciplina vinculada ao curso", matrizService.create(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_CURSOS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        matrizService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculo removido", null));
    }
}
