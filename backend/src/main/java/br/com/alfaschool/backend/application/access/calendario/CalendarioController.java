package br.com.alfaschool.backend.application.access.calendario;

import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioDiaRequest;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioDiaResponse;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioMesResponse;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioRequest;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioResponse;
import br.com.alfaschool.backend.application.access.calendario.dto.IntervaloDiasRequest;
import br.com.alfaschool.backend.security.filter.TenantContext;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/calendarios")
public class CalendarioController {

    private final CalendarioService calendarioService;
    private final CalendarioDiaService calendarioDiaService;
    private final CalendarioConsultaService consultaService;

    public CalendarioController(CalendarioService calendarioService,
                                CalendarioDiaService calendarioDiaService,
                                CalendarioConsultaService consultaService) {
        this.calendarioService = calendarioService;
        this.calendarioDiaService = calendarioDiaService;
        this.consultaService = consultaService;
    }

    // ---------------------------- calendarios ----------------------------

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<Page<CalendarioResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("anoLetivo").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Calendarios listados com sucesso",
                calendarioService.list(pageable)));
    }

    @GetMapping("/ano/{ano}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<List<CalendarioResponse>>> listByAno(@PathVariable Integer ano) {
        return ResponseEntity.ok(ApiResponse.of(200, "Calendarios do ano listados com sucesso",
                calendarioService.listByAno(ano)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Calendario encontrado", calendarioService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioResponse>> create(@Valid @RequestBody CalendarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Calendario criado com sucesso", calendarioService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioResponse>> update(@PathVariable UUID id,
                                                                  @Valid @RequestBody CalendarioRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Calendario atualizado com sucesso",
                calendarioService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        calendarioService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Calendario removido com sucesso", null));
    }

    // ------------------------------- dias --------------------------------

    @GetMapping("/{id}/dias")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<List<CalendarioDiaResponse>>> listDias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dias do calendario listados com sucesso",
                calendarioDiaService.listDias(id)));
    }

    @PostMapping("/{id}/dias")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioDiaResponse>> criarDia(@PathVariable UUID id,
                                                                       @Valid @RequestBody CalendarioDiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Dia lancado com sucesso", calendarioDiaService.salvarDia(id, request)));
    }

    /** Lancamento em lote: recesso, semana de provas, ponte de feriado. */
    @PostMapping("/{id}/dias/intervalo")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<List<CalendarioDiaResponse>>> lancarIntervalo(
            @PathVariable UUID id, @Valid @RequestBody IntervaloDiasRequest request) {
        List<CalendarioDiaResponse> dias = calendarioDiaService.lancarIntervalo(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, dias.size() + " dia(s) lancado(s) com sucesso", dias));
    }

    /** Grade do mes para a tela, com os dias nao lancados ja resolvidos. */
    @GetMapping("/{id}/mes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioMesResponse>> mes(@PathVariable UUID id,
                                                                  @RequestParam int ano,
                                                                  @RequestParam int mes) {
        return ResponseEntity.ok(ApiResponse.of(200, "Calendario do mes carregado com sucesso",
                calendarioDiaService.mes(id, ano, mes)));
    }

    @GetMapping("/dias/{diaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioDiaResponse>> findDia(@PathVariable UUID diaId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dia encontrado", calendarioDiaService.findDia(diaId)));
    }

    @PutMapping("/dias/{diaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<CalendarioDiaResponse>> updateDia(@PathVariable UUID diaId,
                                                                        @Valid @RequestBody CalendarioDiaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dia atualizado com sucesso",
                calendarioDiaService.updateDia(diaId, request)));
    }

    @DeleteMapping("/dias/{diaId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<Void>> deleteDia(@PathVariable UUID diaId) {
        calendarioDiaService.deleteDia(diaId);
        return ResponseEntity.ok(ApiResponse.of(200, "Dia removido com sucesso", null));
    }

    // ------------------------------ consulta ------------------------------

    /** Mesma regra que a apuracao de permanencia usa, exposta para a tela. */
    @GetMapping("/dia-letivo")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_CALENDARIO_GERIR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ehDiaLetivo(
            @RequestParam(required = false) UUID unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        boolean letivo = consultaService.ehDiaLetivo(tenantId, unitId, data);
        var tipo = consultaService.tipoDoDia(tenantId, unitId, data);
        Map<String, Object> corpo = new java.util.LinkedHashMap<>();
        corpo.put("data", data);
        corpo.put("unitId", unitId);
        corpo.put("letivo", letivo);
        corpo.put("tipo", tipo != null ? tipo.name() : null);
        corpo.put("cadastrado", tipo != null);
        return ResponseEntity.ok(ApiResponse.of(200, "Consulta de dia letivo realizada", corpo));
    }
}
