package br.com.alfaschool.backend.application.diario;

import br.com.alfaschool.backend.application.diario.dto.BoletimResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boletim")
public class BoletimController {

    private final BoletimService boletimService;

    public BoletimController(BoletimService boletimService) {
        this.boletimService = boletimService;
    }

    /**
     * Gera o boletim completo de uma matrícula.
     */
    @GetMapping("/{matriculaId}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_NOTAS_VER')")
    public ResponseEntity<ApiResponse<BoletimResponse>> gerarBoletim(
            @PathVariable UUID matriculaId,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(ApiResponse.of(200, "Boletim gerado",
                boletimService.gerarBoletim(matriculaId, periodo)));
    }
}
