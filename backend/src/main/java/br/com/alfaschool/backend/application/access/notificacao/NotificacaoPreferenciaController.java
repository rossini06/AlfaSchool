package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.PreferenciaRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.PreferenciaResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Opt-in por titular. Sem linha aqui com opt_in_em preenchido, nao ha envio. */
@RestController
@RequestMapping("/api/v1/access/notificacoes/preferencias")
public class NotificacaoPreferenciaController {

    private final NotificacaoPreferenciaService service;

    public NotificacaoPreferenciaController(NotificacaoPreferenciaService service) {
        this.service = service;
    }

    @GetMapping("/titular/{titularId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<List<PreferenciaResponse>>> listarPorTitular(@PathVariable UUID titularId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Preferências listadas com sucesso",
                service.listarPorTitular(titularId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<PreferenciaResponse>> salvar(@Valid @RequestBody PreferenciaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Preferência salva com sucesso", service.salvar(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<PreferenciaResponse>> alternar(@PathVariable UUID id,
                                                                     @RequestParam boolean habilitado) {
        return ResponseEntity.ok(ApiResponse.of(200,
                habilitado ? "Opt-in registrado com sucesso" : "Opt-out registrado com sucesso",
                service.alternar(id, habilitado)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Preferência removida com sucesso", null));
    }
}
