package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.EnvioResponse;
import br.com.alfaschool.backend.application.access.notificacao.dto.TesteNotificacaoRequest;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/notificacoes")
public class NotificacaoHistoricoController {

    private final NotificacaoHistoricoService service;

    public NotificacaoHistoricoController(NotificacaoHistoricoService service) {
        this.service = service;
    }

    @GetMapping("/historico")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_VER')")
    public ResponseEntity<ApiResponse<Page<EnvioResponse>>> historico(
            @RequestParam(required = false) CanalNotificacao canal,
            @RequestParam(required = false) EventoNotificacao evento,
            @RequestParam(required = false) StatusEnvio status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim,
            @RequestParam(required = false) String destino,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Histórico listado com sucesso",
                service.historico(canal, evento, status, inicio, fim, destino, pageable)));
    }

    @GetMapping("/historico/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_VER')")
    public ResponseEntity<ApiResponse<EnvioResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Envio encontrado", service.buscar(id)));
    }

    @PostMapping("/historico/{id}/reenviar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<EnvioResponse>> reenviar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Reenvio enfileirado com sucesso", service.reenviar(id)));
    }

    @PostMapping("/teste")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_NOTIFICACOES_CONFIGURAR')")
    public ResponseEntity<ApiResponse<EnvioResponse>> teste(@Valid @RequestBody TesteNotificacaoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Mensagem de teste enfileirada com sucesso",
                service.teste(request)));
    }
}
