package br.com.alfaschool.backend.application.access.agent;

import br.com.alfaschool.backend.application.access.agent.dto.AgentDtos;
import br.com.alfaschool.backend.domain.access.equipamento.AccAgentCredencial;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import br.com.alfaschool.backend.security.jwt.JwtProperties;
import br.com.alfaschool.backend.security.jwt.JwtTokenProvider;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Protocolo do agente local.
 *
 * AUTENTICACAO: /agent/login e' a UNICA rota publica desta classe e
 * precisa ser liberada no SecurityConfig (ver relatorio). As demais usam
 * o JWT emitido por ela, validado pelo JwtAuthenticationFilter que ja
 * existe — nao foi preciso criar filtro proprio nem tocar no
 * JwtTokenProvider: o token do agente e' um access token comum, com a
 * role AGENT e o tenant do agente, e o TenantFilter ja resolve o
 * TenantContext a partir dele.
 *
 * A role AGENT nao concede nada alem destas rotas: cada metodo exige
 * hasRole('AGENT') explicitamente. Reportado no relatorio o que precisa
 * ser conferido no SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/access/agent")
public class AgentController {

    public static final String ROLE_AGENT = "AGENT";

    private final AgentCredencialService credenciais;
    private final AgentService service;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProperties;

    public AgentController(AgentCredencialService credenciais,
                           AgentService service,
                           JwtTokenProvider jwt,
                           JwtProperties jwtProperties) {
        this.credenciais = credenciais;
        this.service = service;
        this.jwt = jwt;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Login do agente.
     *
     * clientId e' o tenant: o username so' e' unico dentro dele. Sem o
     * clientId, "agente-portaria" de uma escola casaria com o de outra.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AgentDtos.LoginResponse>> login(
            @Valid @RequestBody AgentDtos.LoginRequest req) {
        Optional<AccAgentCredencial> c =
                credenciais.autenticar(req.clientId(), req.username(), req.password());
        if (c.isEmpty()) {
            // Mesma resposta para usuario inexistente e senha errada.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        AccAgentCredencial cred = c.get();
        String token = jwt.generateAccessToken(
                cred.getId(), cred.getTenantId(), cred.getUnitId(), List.of(ROLE_AGENT));
        Instant expira = Instant.now().plus(jwtProperties.jwtExpirationMinutes(), ChronoUnit.MINUTES);
        return ResponseEntity.ok(ApiResponse.of(200, "Autenticado.",
                new AgentDtos.LoginResponse(token, expira, cred.getTenantId(), cred.getUnitId())));
    }

    @GetMapping("/devices")
    @PreAuthorize("isAuthenticated() and hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<AgentDtos.DeviceDto>>> devices(
            @RequestParam(defaultValue = "false") boolean incluirSenha) {
        return ResponseEntity.ok(ApiResponse.of(200, "Equipamentos da unidade.",
                service.devices(tenant(), unidadeDoAgente(), incluirSenha)));
    }

    @GetMapping("/users")
    @PreAuthorize("isAuthenticated() and hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentDtos.UsersPage>> users(
            @RequestParam(required = false) Instant updatedAfter) {
        return ResponseEntity.ok(ApiResponse.of(200, "Delta de pessoas.",
                service.users(tenant(), updatedAfter)));
    }

    @PostMapping("/events")
    @PreAuthorize("isAuthenticated() and hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentDtos.EventResponse>> evento(
            @Valid @RequestBody AgentDtos.EventRequest req) {
        AgentDtos.EventResponse r = service.evento(tenant(), req);
        // 200 tambem no replay: o agente precisa poder avancar o cursor.
        // Devolver erro faria a fila offline travar no mesmo evento.
        return ResponseEntity.ok(ApiResponse.of(200,
                r.replay() ? "Evento já registrado (replay)." : "Evento registrado.", r));
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("isAuthenticated() and hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @Valid @RequestBody AgentDtos.HeartbeatRequest req) {
        service.heartbeat(tenant(), req);
        return ResponseEntity.ok(ApiResponse.of(200, "Heartbeat registrado.", null));
    }

    @GetMapping("/tasks/pending")
    @PreAuthorize("isAuthenticated() and hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<AgentDtos.TaskDto>>> pendentes(
            @RequestParam(required = false) UUID dispositivoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Tarefas pendentes.",
                service.tarefasPendentes(tenant(), dispositivoId)));
    }

    @PutMapping("/tasks/{id}/result")
    @PreAuthorize("isAuthenticated() and hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentDtos.TaskDto>> resultado(
            @PathVariable UUID id,
            @RequestBody AgentDtos.TaskResultRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Resultado registrado.",
                service.resultado(tenant(), id, req)));
    }

    private UUID tenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado.");
        }
        return tenantId;
    }

    /** Agente com unitId null enxerga a escola inteira. */
    private UUID unidadeDoAgente() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return u.unitId();
        }
        return null;
    }
}
