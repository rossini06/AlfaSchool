package br.com.alfaschool.backend.application.user;

import br.com.alfaschool.backend.application.user.dto.CreateUserRequest;
import br.com.alfaschool.backend.application.user.dto.UserResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.alfaschool.backend.application.user.dto.AtualizarUsuarioRequest;
import br.com.alfaschool.backend.shared.web.Paginacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * Listagem. Nao existia: a tela de Usuarios chamava a API e recebia 404,
     * entao nunca mostrou uma linha desde que foi escrita.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_VER')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Usuários listados com sucesso",
                userApplicationService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_VER')")
    public ResponseEntity<ApiResponse<UserResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Usuário encontrado",
                userApplicationService.buscar(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<UserResponse>> atualizar(
            @PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Usuário atualizado com sucesso",
                userApplicationService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable UUID id) {
        userApplicationService.excluir(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Usuário excluído", null));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userApplicationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Usuário criado com sucesso", response));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_GERIR')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        UserResponse response = userApplicationService.assignRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.of(200, "Role atribuída com sucesso", response));
    }

    @GetMapping("/{userId}/roles")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_USUARIOS_VER')")
    public ResponseEntity<ApiResponse<List<String>>> userRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Roles listadas com sucesso", userApplicationService.userRoles(userId)));
    }
}
