package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoConfigRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoConfigResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Configuracao dos canais de saida. O segredo entra em claro e sai mascarado. */
@RestController
@RequestMapping("/api/v1/access/notificacoes/configs")
public class NotificacaoConfigController {

    private final NotificacaoConfigService service;

    public NotificacaoConfigController(NotificacaoConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<List<NotificacaoConfigResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.of(200, "Configurações listadas com sucesso", service.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<NotificacaoConfigResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Configuração encontrada", service.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<NotificacaoConfigResponse>> criar(
            @Valid @RequestBody NotificacaoConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Configuração criada com sucesso", service.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<NotificacaoConfigResponse>> atualizar(
            @PathVariable UUID id, @Valid @RequestBody NotificacaoConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Configuração atualizada com sucesso",
                service.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Configuração removida com sucesso", null));
    }
}
