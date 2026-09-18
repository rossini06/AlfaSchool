package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoTemplateRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoTemplateResponse;
import br.com.alfaschool.backend.application.access.notificacao.dto.TemplatePreviewRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.TemplatePreviewResponse;
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

@RestController
@RequestMapping("/api/v1/access/notificacoes/templates")
public class NotificacaoTemplateController {

    private final NotificacaoTemplateService service;

    public NotificacaoTemplateController(NotificacaoTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<List<NotificacaoTemplateResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.of(200, "Templates listados com sucesso", service.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<NotificacaoTemplateResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Template encontrado", service.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<NotificacaoTemplateResponse>> criar(
            @Valid @RequestBody NotificacaoTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Template criado com sucesso", service.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<NotificacaoTemplateResponse>> atualizar(
            @PathVariable UUID id, @Valid @RequestBody NotificacaoTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Template atualizado com sucesso",
                service.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Template removido com sucesso", null));
    }

    /** Preview renderizado: mostra ao operador exatamente o que a familia vai ler. */
    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<TemplatePreviewResponse>> preview(
            @RequestBody TemplatePreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Preview gerado com sucesso", service.preview(request)));
    }
}
