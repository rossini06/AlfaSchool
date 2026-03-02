package br.com.alfaschool.backend.application.auth;

import br.com.alfaschool.backend.application.auth.dto.AuthTokensResponse;
import br.com.alfaschool.backend.application.auth.dto.LoginRequest;
import br.com.alfaschool.backend.application.auth.dto.RefreshRequest;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                 jakarta.servlet.http.HttpServletRequest httpRequest) {
        AuthTokensResponse response = authApplicationService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.of(200, "Login realizado com sucesso", response));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthTokensResponse response = authApplicationService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.of(200, "Token renovado com sucesso", response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.of(200, "OK", Map.of("status", "ok")));
    }
}
