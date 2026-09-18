package br.com.alfaschool.backend.application.access.agent;

import br.com.alfaschool.backend.application.access.agent.dto.AgentDtos;
import br.com.alfaschool.backend.domain.access.equipamento.AccAgentCredencial;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAgentCredencialRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestao das credenciais de agente, pela equipe da escola.
 *
 * Fica FORA de /api/v1/access/agent/** de proposito: aquele prefixo vai
 * ser liberado no SecurityConfig para o login do agente, e criar
 * credencial nao pode cair junto.
 */
@RestController
@RequestMapping("/api/v1/access/agent-credenciais")
public class AgentCredencialController {

    private final AgentCredencialService service;
    private final AccAgentCredencialRepository repository;

    public AgentCredencialController(AgentCredencialService service,
                                     AccAgentCredencialRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<AgentDtos.CredencialCriadaDto>> criar(
            @Valid @RequestBody AgentDtos.NovaCredencialRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.of(201, "Credencial criada. A senha só será exibida agora.",
                        service.criar(tenant(), req)));
    }

    /** Lista sem hash nem senha: nada aqui permite reconstruir a credencial. */
    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listar() {
        List<Map<String, Object>> lista = repository.findByTenantIdAndDeletedFalse(tenant())
                .stream()
                .map(c -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("username", c.getUsername());
                    m.put("unitId", c.getUnitId());
                    m.put("descricao", c.getDescricao());
                    m.put("ativo", c.isAtivo());
                    m.put("ultimoLogin", c.getUltimoLogin());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.of(200, "Credenciais de agente.", lista));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> desativar(@PathVariable UUID id) {
        AccAgentCredencial c = repository.findByIdAndTenantIdAndDeletedFalse(id, tenant())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Credencial não encontrada."));
        // Desativa em vez de apagar: o historico de ultimo_login e' parte
        // da trilha de auditoria de quem sincronizou biometria.
        c.setAtivo(false);
        repository.save(c);
        return ResponseEntity.ok(ApiResponse.of(200, "Credencial desativada.", null));
    }

    private UUID tenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado.");
        }
        return tenantId;
    }
}
