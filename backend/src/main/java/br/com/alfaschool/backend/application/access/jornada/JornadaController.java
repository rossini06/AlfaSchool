package br.com.alfaschool.backend.application.access.jornada;

import br.com.alfaschool.backend.application.access.jornada.dto.*;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/jornadas")
public class JornadaController {

    private final JornadaService jornadaService;

    public JornadaController(JornadaService jornadaService) {
        this.jornadaService = jornadaService;
    }

    // ---------------------------------------------------------------- jornadas

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Page<JornadaResponse>>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Jornadas listadas com sucesso",
                jornadaService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<JornadaResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Jornada encontrada", jornadaService.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<JornadaResponse>> criar(@Valid @RequestBody JornadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Jornada criada com sucesso", jornadaService.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<JornadaResponse>> atualizar(@PathVariable UUID id,
                                                                  @Valid @RequestBody JornadaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Jornada atualizada com sucesso",
                jornadaService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable UUID id) {
        jornadaService.remover(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Jornada removida com sucesso", null));
    }

    // ---------------------------------------------------------------- vinculos

    @GetMapping("/vinculos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Page<AlunoJornadaResponse>>> listarVinculos(
            @RequestParam UUID alunoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("vigenciaInicio").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculos listados com sucesso",
                jornadaService.listarVinculos(alunoId, pageable)));
    }

    /** Qual jornada valia para o aluno naquela data — a base da apuracao. */
    @GetMapping("/vigente")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<AlunoJornadaResponse>> vigente(
            @RequestParam UUID alunoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return jornadaService.jornadaVigenteDoAluno(alunoId, data)
                .map(v -> ResponseEntity.ok(ApiResponse.of(200, "Jornada vigente encontrada", v)))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponse.of(200, "Aluno sem jornada vigente na data", null)));
    }

    @PostMapping("/vinculos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<AlunoJornadaResponse>> vincular(
            @Valid @RequestBody AlunoJornadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Vínculo criado com sucesso", jornadaService.vincular(request)));
    }

    @PutMapping("/vinculos/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<AlunoJornadaResponse>> atualizarVinculo(
            @PathVariable UUID id, @Valid @RequestBody AlunoJornadaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculo atualizado com sucesso",
                jornadaService.atualizarVinculo(id, request)));
    }

    @DeleteMapping("/vinculos/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> removerVinculo(@PathVariable UUID id) {
        jornadaService.removerVinculo(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Vínculo removido com sucesso", null));
    }

    /** Matricular varios alunos no mesmo plano — "a turma toda no plano de 5h". */
    @PostMapping("/aplicar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<AplicarJornadaResponse>> aplicarEmLote(
            @Valid @RequestBody AplicarJornadaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Jornada aplicada aos alunos",
                jornadaService.aplicarEmLote(request)));
    }

    /** Alunos de uma turma, para montar o lote no front sem adivinhacao. */
    @GetMapping("/alunos-da-turma")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<List<UUID>>> alunosDaTurma(@RequestParam UUID turmaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Alunos da turma listados",
                jornadaService.alunosDaTurma(turmaId)));
    }

    // ---------------------------------------------------------------- excecoes

    @GetMapping("/excecoes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<List<JornadaExcecaoResponse>>> listarExcecoes(
            @RequestParam UUID alunoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(ApiResponse.of(200, "Exceções listadas com sucesso",
                jornadaService.listarExcecoes(alunoId, inicio, fim)));
    }

    @PostMapping("/excecoes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<JornadaExcecaoResponse>> salvarExcecao(
            @Valid @RequestBody JornadaExcecaoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Exceção registrada com sucesso",
                jornadaService.salvarExcecao(request)));
    }

    @DeleteMapping("/excecoes/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> removerExcecao(@PathVariable UUID id) {
        jornadaService.removerExcecao(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Exceção removida com sucesso", null));
    }
}
