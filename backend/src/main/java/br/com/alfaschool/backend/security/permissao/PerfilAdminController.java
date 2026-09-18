package br.com.alfaschool.backend.security.permissao;

import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Administracao de perfis e permissoes pela propria escola.
 *
 * Existe para que a instituicao ajuste quem pode o que sem depender de
 * alteracao de codigo. O conjunto que vem semeado e' ponto de partida,
 * nao camisa de forca: escola nenhuma organiza o trabalho igual a outra.
 */
@RestController
@RequestMapping("/api/v1/perfis")
public class PerfilAdminController {

    private final PerfilAdminService service;

    public PerfilAdminController(PerfilAdminService service) {
        this.service = service;
    }

    public record AtualizarPermissoesRequest(
            @NotNull(message = "permissoes é obrigatório.") List<String> permissoes) {
    }

    public record PermissoesExtrasRequest(
            @NotNull(message = "permissoes é obrigatório.") List<String> permissoes,
            String motivo) {
    }

    /** Catalogo completo, agrupado por area, para montar a matriz. */
    @GetMapping("/catalogo")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PERFIS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> catalogo() {
        return ResponseEntity.ok(ApiResponse.of(200, "Catálogo de permissões", service.catalogo()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PERFIS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> listar() {
        return ResponseEntity.ok(ApiResponse.of(200, "Perfis da escola", service.listar()));
    }

    @PutMapping("/{id}/permissoes")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_PERFIS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> atualizar(@PathVariable UUID id,
                                                         @Valid @RequestBody AtualizarPermissoesRequest req) {
        service.atualizarPermissoes(id, req.permissoes());
        return ResponseEntity.ok(ApiResponse.of(200,
                "Permissões salvas. Valem a partir do próximo login de quem tem este perfil.", null));
    }

    /** O que uma pessoa tem: pelo perfil, e o que foi concedido a ela. */
    @GetMapping("/usuarios/{userId}/permissoes")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> doUsuario(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Permissões do usuário", service.doUsuario(userId)));
    }

    @PutMapping("/usuarios/{userId}/permissoes")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<Object>> atualizarExtras(@PathVariable UUID userId,
                                                               @Valid @RequestBody PermissoesExtrasRequest req) {
        service.substituirExtras(userId, req.permissoes(), req.motivo());
        return ResponseEntity.ok(ApiResponse.of(200,
                "Permissões salvas. Valem a partir do próximo login do usuário.", null));
    }
}
