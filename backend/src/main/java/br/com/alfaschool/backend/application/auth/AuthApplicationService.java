package br.com.alfaschool.backend.application.auth;

import br.com.alfaschool.backend.application.auth.dto.AuthTokensResponse;
import br.com.alfaschool.backend.application.auth.dto.LoginRequest;
import br.com.alfaschool.backend.application.shared.AuditService;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthApplicationService {

    private static final int MAX_TENTATIVAS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public AuthApplicationService(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider,
                                  AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthTokensResponse login(LoginRequest request, String ipAddress) {
        UserAccount user = resolveUserForLogin(request);

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");
        }

        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Conta bloqueada por tentativas inválidas");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailedAttempt(user, ipAddress);
            throw credenciaisInvalidas();
        }

        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        List<String> roles = user.getRoles().stream().map(role -> role.getName().toUpperCase()).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getUnitId(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTenantId());

        auditService.register(user.getTenantId(), user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), ipAddress);

        return new AuthTokensResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getTenantId(),
                roles,
                user.isMustChangePassword()
        );
    }

    public AuthTokensResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido");
        }

        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        UUID userId = UUID.fromString(claims.get("userId", String.class));
        UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));

        UserAccount user = userRepository.findById(userId)
                .filter(it -> it.getTenantId().equals(tenantId))
                .orElseThrow(this::credenciaisInvalidas);

        List<String> roles = user.getRoles().stream().map(role -> role.getName().toUpperCase()).toList();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getUnitId(), roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTenantId());

        return new AuthTokensResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getTenantId(),
                roles,
                user.isMustChangePassword()
        );
    }

    private void registerFailedAttempt(UserAccount user, String ipAddress) {
        int tentativas = user.getFailedAttempts() + 1;
        user.setFailedAttempts(tentativas);
        if (tentativas >= MAX_TENTATIVAS) {
            user.setLocked(true);
        }
        userRepository.save(user);
        auditService.register(user.getTenantId(), user.getId(), "LOGIN_FAILED", "USER", user.getId(), ipAddress);
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Essas credenciais não foram encontradas em nossos registros.");
    }

    private UserAccount resolveUserForLogin(LoginRequest request) {
        if (request.tenantId() != null) {
            return userRepository.findByTenantIdAndEmailIgnoreCase(request.tenantId(), request.email())
                    .orElseGet(() -> findSuperAdminByEmail(request.email()).orElseThrow(this::credenciaisInvalidas));
        }

        return findSuperAdminByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tenant ID é obrigatório para usuários comuns. Super Admin pode acessar sem tenant."));
    }

    private java.util.Optional<UserAccount> findSuperAdminByEmail(String email) {
        return userRepository.findAllByEmailIgnoreCase(email).stream()
                .filter(user -> user.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName())))
                .findFirst();
    }
}
