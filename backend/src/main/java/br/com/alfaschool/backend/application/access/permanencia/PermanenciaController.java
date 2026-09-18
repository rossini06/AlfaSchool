package br.com.alfaschool.backend.application.access.permanencia;

import br.com.alfaschool.backend.application.access.permanencia.dto.*;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/permanencia")
public class PermanenciaController {

    private final PermanenciaService permanenciaService;

    public PermanenciaController(PermanenciaService permanenciaService) {
        this.permanenciaService = permanenciaService;
    }

    /** Quem esta na unidade agora. */
    @GetMapping("/hoje")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Page<PresencaResponse>>> hoje(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.of(200, "Alunos presentes listados com sucesso",
                permanenciaService.quemEstaNaUnidade(unitId, turmaId, pageable)));
    }

    /** Extrato diario do aluno, com os totais do periodo inteiro. */
    @GetMapping("/aluno/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<ExtratoAlunoResponse>> extrato(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "31") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("data").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Extrato gerado com sucesso",
                permanenciaService.extrato(id, inicio, fim, pageable)));
    }

    @GetMapping("/excedentes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Page<TotaisAlunoResponse>>> excedentes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.of(200, "Excedentes listados com sucesso",
                permanenciaService.excedentes(inicio, fim, unitId, turmaId, pageable)));
    }

    @GetMapping("/resumo")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<ResumoPermanenciaResponse>> resumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(required = false) UUID unitId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Resumo gerado com sucesso",
                permanenciaService.resumo(inicio, fim, turmaId, unitId)));
    }

    /** Ajuste manual de par. Exige motivo e grava quem ajustou. */
    @PostMapping("/ajustes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<PresencaResponse>> ajustar(@Valid @RequestBody AjusteParRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Ajuste aplicado com sucesso",
                permanenciaService.ajustarPar(request)));
    }

    // ------------------------------------------------------------- fechamento

    @GetMapping("/fechamentos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Page<FechamentoResponse>>> listarFechamentos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("competencia").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Fechamentos listados com sucesso",
                permanenciaService.listarFechamentos(pageable)));
    }

    @PostMapping("/fechamentos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and (hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPER_ADMIN'))")
    public ResponseEntity<ApiResponse<FechamentoResponse>> fechar(@Valid @RequestBody FechamentoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Competência fechada com sucesso",
                permanenciaService.fechar(request)));
    }

    /**
     * Reabrir muda o valor de faturas ja emitidas: exige papel
     * administrativo e fica registrado em audit_logs.
     */
    @PostMapping("/fechamentos/{id}/reabrir")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and (hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPER_ADMIN'))")
    public ResponseEntity<ApiResponse<FechamentoResponse>> reabrir(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Competência reaberta com sucesso",
                permanenciaService.reabrir(id)));
    }

    // -------------------------------------------------------- reprocessamento

    /** Recalculo administrativo de um periodo. Dia congelado nao e' tocado. */
    @PostMapping("/recalcular")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and (hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPER_ADMIN'))")
    public ResponseEntity<ApiResponse<RecalculoResponse>> recalcular(@Valid @RequestBody RecalculoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Recálculo concluído",
                permanenciaService.recalcularPeriodo(request)));
    }
}
