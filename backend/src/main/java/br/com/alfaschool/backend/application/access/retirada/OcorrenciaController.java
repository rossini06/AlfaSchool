package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.dto.OcorrenciaRequest;
import br.com.alfaschool.backend.application.access.retirada.dto.OcorrenciaResponse;
import br.com.alfaschool.backend.application.access.retirada.dto.TratativaRequest;
import br.com.alfaschool.backend.domain.access.retirada.GravidadeOcorrencia;
import br.com.alfaschool.backend.domain.access.retirada.StatusOcorrencia;
import br.com.alfaschool.backend.domain.access.shared.TipoOcorrencia;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Page<OcorrenciaResponse>>> listar(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) TipoOcorrencia tipo,
            @RequestParam(required = false) GravidadeOcorrencia gravidade,
            @RequestParam(required = false) StatusOcorrencia status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.of(200, "Ocorrencias listadas com sucesso",
                ocorrenciaService.listar(unitId, tipo, gravidade, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<OcorrenciaResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Ocorrencia encontrada", ocorrenciaService.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<OcorrenciaResponse>> criar(@Valid @RequestBody OcorrenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Ocorrencia registrada com sucesso", ocorrenciaService.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<OcorrenciaResponse>> atualizar(@PathVariable UUID id,
                                                                      @Valid @RequestBody OcorrenciaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Ocorrencia atualizada com sucesso",
                ocorrenciaService.atualizar(id, request)));
    }

    @PostMapping("/{id}/tratar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<OcorrenciaResponse>> tratar(@PathVariable UUID id,
                                                                   @Valid @RequestBody TratativaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Tratativa registrada com sucesso",
                ocorrenciaService.tratar(id, request)));
    }

    @PostMapping("/{id}/fechar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<OcorrenciaResponse>> fechar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Ocorrencia fechada com sucesso", ocorrenciaService.fechar(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable UUID id) {
        ocorrenciaService.remover(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Ocorrencia removida com sucesso", null));
    }
}
