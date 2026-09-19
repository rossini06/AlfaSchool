package br.com.alfaschool.backend.application.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.dto.SalaVigenteResponse;
import br.com.alfaschool.backend.application.access.estrutura.dto.TurmaSalaRequest;
import br.com.alfaschool.backend.application.access.estrutura.dto.TurmaSalaResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/turma-salas")
public class TurmaSalaController {

    private final TurmaSalaService turmaSalaService;

    public TurmaSalaController(TurmaSalaService turmaSalaService) {
        this.turmaSalaService = turmaSalaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<Page<TurmaSalaResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("vigenciaInicio").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Vinculos turma-sala listados com sucesso",
                turmaSalaService.list(pageable)));
    }

    @GetMapping("/turma/{turmaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<List<TurmaSalaResponse>>> listByTurma(@PathVariable UUID turmaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Vinculos da turma listados com sucesso",
                turmaSalaService.listByTurma(turmaId)));
    }

    @GetMapping("/sala/{salaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<List<TurmaSalaResponse>>> listBySala(@PathVariable UUID salaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Vinculos da sala listados com sucesso",
                turmaSalaService.listBySala(salaId)));
    }

    /** Onde a turma esta no instante informado (ou agora, se nao informado). */
    @GetMapping("/turma/{turmaId}/sala-vigente")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<SalaVigenteResponse>> salaVigente(
            @PathVariable UUID turmaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime momento) {
        LocalDateTime instante = momento != null ? momento : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.of(200, "Sala vigente resolvida com sucesso",
                turmaSalaService.salaVigenteDaTurma(turmaId, instante)));
    }

    @GetMapping("/sala/{salaId}/turmas-no-momento")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<List<TurmaSalaResponse>>> turmasNaSala(
            @PathVariable UUID salaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime momento) {
        LocalDateTime instante = momento != null ? momento : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.of(200, "Turmas na sala listadas com sucesso",
                turmaSalaService.turmasNaSala(salaId, instante)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<TurmaSalaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Vinculo encontrado", turmaSalaService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<TurmaSalaResponse>> create(@Valid @RequestBody TurmaSalaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Vinculo turma-sala criado com sucesso", turmaSalaService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<TurmaSalaResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody TurmaSalaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Vinculo turma-sala atualizado com sucesso",
                turmaSalaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        turmaSalaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Vinculo turma-sala removido com sucesso", null));
    }
}
