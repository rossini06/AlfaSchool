package br.com.alfaschool.backend.application.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.dto.SalaRequest;
import br.com.alfaschool.backend.application.access.estrutura.dto.SalaResponse;
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
@RequestMapping("/api/v1/access/salas")
public class SalaController {

    private final SalaService salaService;
    private final TurmaSalaService turmaSalaService;

    public SalaController(SalaService salaService, TurmaSalaService turmaSalaService) {
        this.salaService = salaService;
        this.turmaSalaService = turmaSalaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<Page<SalaResponse>>> list(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("nome").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Salas listadas com sucesso",
                salaService.list(unitId, pageable)));
    }

    @GetMapping("/zona/{zonaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<List<SalaResponse>>> listByZona(@PathVariable UUID zonaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Salas da zona listadas com sucesso",
                salaService.listByZona(zonaId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<SalaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Sala encontrada", salaService.findById(id)));
    }

    /** Quem esta nesta sala agora. Sem "momento", assume o instante da chamada. */
    @GetMapping("/{id}/turmas")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<List<TurmaSalaResponse>>> turmasNaSala(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime momento) {
        LocalDateTime instante = momento != null ? momento : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.of(200, "Turmas na sala listadas com sucesso",
                turmaSalaService.turmasNaSala(id, instante)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<SalaResponse>> create(@Valid @RequestBody SalaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Sala criada com sucesso", salaService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<SalaResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody SalaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Sala atualizada com sucesso", salaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        salaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Sala removida com sucesso", null));
    }
}
