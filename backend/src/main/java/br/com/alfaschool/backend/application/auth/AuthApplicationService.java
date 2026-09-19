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

import br.com.alfaschool.backend.security.permissao.SuperAdminGuard;

@Service
public class AuthApplicationService {

    private final br.com.alfaschool.backend.security.permissao.PermissaoService permissaoService;

    private static final int MAX_TENTATIVAS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;
    private final SuperAdminGuard superAdminGuard;

    public AuthApplicationService(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider,
                                  AuditService auditService,
                                  br.com.alfaschool.backend.security.permissao.PermissaoService permissaoService,
                                  SuperAdminGuard superAdminGuard) {
        this.permissaoService = permissaoService;
        this.superAdminGuard = superAdminGuard;
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

        // Passa pelo guard: um perfil chamado SUPER_ADMIN fora do tenant
        // mestre nao pode virar authority ROLE_SUPER_ADMIN no token.
        List<String> roles = superAdminGuard.papeisParaToken(user);
        List<String> permissoes = permissaoService.nomesDe(user);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getTenantId(), user.getUnitId(), roles, permissoes);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTenantId());

        auditService.register(user.getTenantId(), user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), ipAddress);

        return new AuthTokensResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getTenantId(),
                roles,
                permissoes,
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

        // Passa pelo guard: um perfil chamado SUPER_ADMIN fora do tenant
        // mestre nao pode virar authority ROLE_SUPER_ADMIN no token.
        List<String> roles = superAdminGuard.papeisParaToken(user);
        List<String> permissoes = permissaoService.nomesDe(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getTenantId(), user.getUnitId(), roles, permissoes);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTenantId());

        return new AuthTokensResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getTenantId(),
                roles,
                permissoes,
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

    /**
     * Descobre quem esta entrando.
     *
     * <p>Com tenant informado, e' busca direta. SEM tenant, resolvemos pelo
     * e-mail: exigir que um professor digite o UUID da escola para entrar
     * nao e' login, e' senha dupla. Como e-mail e' unico dentro de uma
     * escola e quase sempre unico entre elas, isso resolve o caso real.
     *
     * <p>Quando o mesmo e-mail existe em MAIS DE UMA escola — a diretora
     * que responde por duas unidades —, ai sim pedimos o tenant, porque
     * escolher um por conta propria poderia logar a pessoa na escola
     * errada. SUPER_ADMIN continua tendo precedencia.
     */
    private UserAccount resolveUserForLogin(LoginRequest request) {
        if (request.tenantId() != null) {
            return userRepository.findByTenantIdAndEmailIgnoreCase(request.tenantId(), request.email())
                    .orElseGet(() -> findSuperAdminByEmail(request.email()).orElseThrow(this::credenciaisInvalidas));
        }

        java.util.Optional<UserAccount> superAdmin = findSuperAdminByEmail(request.email());
        if (superAdmin.isPresent()) {
            return superAdmin.get();
        }

        java.util.List<UserAccount> candidatos = userRepository.findAllByEmailIgnoreCase(request.email()).stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .toList();

        if (candidatos.size() == 1) {
            return candidatos.get(0);
        }
        if (candidatos.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este e-mail está cadastrado em mais de uma escola. Informe qual escola deseja acessar.");
        }
        // Nenhum candidato: mesma mensagem de senha errada, para nao revelar
        // quais e-mails existem no sistema.
        throw credenciaisInvalidas();
    }

    /**
     * Pelo guard, e nao pelo nome do perfil: senao um perfil "SUPER_ADMIN"
     * criado dentro de uma escola ganharia a precedencia que existe para o
     * administrador do sistema — e, de quebra, driblaria o 409 de e-mail
     * repetido em mais de uma escola.
     */
    private java.util.Optional<UserAccount> findSuperAdminByEmail(String email) {
        return userRepository.findAllByEmailIgnoreCase(email).stream()
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .filter(superAdminGuard::ehSuperAdmin)
                .findFirst();
    }
}
