package br.com.alfaschool.backend.application.responsavel;

import br.com.alfaschool.backend.application.responsavel.dto.AlunoResponsavelRequest;
import br.com.alfaschool.backend.application.responsavel.dto.AlunoResponsavelResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alunos/{alunoId}/responsaveis")
public class AlunoResponsavelController {

    private final AlunoResponsavelService alunoResponsavelService;

    public AlunoResponsavelController(AlunoResponsavelService alunoResponsavelService) {
        this.alunoResponsavelService = alunoResponsavelService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_RESPONSAVEIS_VER')")
    public ResponseEntity<ApiResponse<List<AlunoResponsavelResponse>>> list(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Responsáveis do aluno listados",
                alunoResponsavelService.listByAluno(alunoId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_RESPONSAVEIS_GERIR')")
    public ResponseEntity<ApiResponse<AlunoResponsavelResponse>> add(
            @PathVariable UUID alunoId,
            @Valid @RequestBody AlunoResponsavelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Responsável vinculado",
                        alunoResponsavelService.addLink(alunoId, request)));
    }

    @PutMapping("/{linkId}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_RESPONSAVEIS_GERIR')")
    public ResponseEntity<ApiResponse<AlunoResponsavelResponse>> update(
            @PathVariable UUID alunoId,
            @PathVariable UUID linkId,
            @Valid @RequestBody AlunoResponsavelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculo atualizado",
                alunoResponsavelService.updateLink(alunoId, linkId, request)));
    }

    @DeleteMapping("/{linkId}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_RESPONSAVEIS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable UUID alunoId,
            @PathVariable UUID linkId) {
        alunoResponsavelService.removeLink(alunoId, linkId);
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculo removido", null));
    }
}
