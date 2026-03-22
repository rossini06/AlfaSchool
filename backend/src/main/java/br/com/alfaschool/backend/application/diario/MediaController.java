package br.com.alfaschool.backend.application.diario;

import br.com.alfaschool.backend.application.diario.dto.MediaResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medias")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Calcula a média do aluno em uma disciplina específica.
     */
    @PostMapping("/calcular")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MediaResponse>> calcularMedia(
            @RequestParam UUID matriculaId,
            @RequestParam UUID disciplinaId,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(ApiResponse.of(200, "Média calculada",
                mediaService.calcularMedia(matriculaId, disciplinaId, periodo)));
    }

    /**
     * Recalcula todas as médias de uma matrícula.
     */
    @PostMapping("/recalcular/{matriculaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> recalcularTodas(@PathVariable UUID matriculaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Médias recalculadas",
                mediaService.recalcularTodasMedias(matriculaId)));
    }

    /**
     * Lista médias de uma turma/disciplina.
     */
    @GetMapping("/turma")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> listByTurma(
            @RequestParam UUID turmaId,
            @RequestParam UUID disciplinaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Médias da turma",
                mediaService.listByTurma(turmaId, disciplinaId)));
    }

    /**
     * Lista médias de uma matrícula.
     */
    @GetMapping("/matricula/{matriculaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> listByMatricula(@PathVariable UUID matriculaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Médias da matrícula",
                mediaService.listByMatricula(matriculaId)));
    }
}
