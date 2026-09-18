package br.com.alfaschool.backend.application.user;

import br.com.alfaschool.backend.application.user.dto.CreateUserRequest;
import br.com.alfaschool.backend.application.user.dto.UserResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
