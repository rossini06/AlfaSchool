package br.com.alfaschool.backend.application.frequencia;

import br.com.alfaschool.backend.application.diario.dto.FrequenciaLoteRequest;
import br.com.alfaschool.backend.application.frequencia.dto.FrequenciaRequest;
import br.com.alfaschool.backend.application.frequencia.dto.FrequenciaResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/frequencias")
public class FrequenciaController {

    private final FrequenciaService frequenciaService;

    public FrequenciaController(FrequenciaService frequenciaService) {
        this.frequenciaService = frequenciaService;
    }

    @GetMapping("/aluno/{alunoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FrequenciaResponse>>> listByAluno(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Frequências do aluno", frequenciaService.listByAluno(alunoId)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FrequenciaResponse>>> listByTurma(
            @RequestParam UUID turmaId,
            @RequestParam UUID disciplinaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(ApiResponse.of(200, "Frequências",
                frequenciaService.listByTurmaAndDisciplina(turmaId, disciplinaId, inicio, fim)));
    }

    @GetMapping("/diario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FrequenciaResponse>>> listByDia(
            @RequestParam UUID turmaId,
            @RequestParam UUID disciplinaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(ApiResponse.of(200, "Frequências do dia",
                frequenciaService.listByTurmaAndDisciplinaAndData(turmaId, disciplinaId, data)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FrequenciaResponse>> registrar(@Valid @RequestBody FrequenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Frequência registrada", frequenciaService.registrar(request)));
    }

    @PostMapping("/lote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FrequenciaResponse>>> registrarLote(
            @Valid @RequestBody FrequenciaLoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Frequências registradas em lote", frequenciaService.registrarLote(request)));
    }

    @PostMapping("/marcar-todos-presentes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FrequenciaResponse>>> marcarTodosPresentes(
            @RequestParam UUID turmaId,
            @RequestParam UUID disciplinaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Integer numeroAula) {
        return ResponseEntity.ok(ApiResponse.of(200, "Todos marcados como presentes",
                frequenciaService.marcarTodosPresentes(turmaId, disciplinaId, data, numeroAula)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FrequenciaResponse>> atualizar(@PathVariable UUID id,
            @Valid @RequestBody FrequenciaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Frequência atualizada", frequenciaService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        frequenciaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Frequência removida", null));
    }
}
