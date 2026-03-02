package br.com.alfaschool.backend.auth;

import br.com.alfaschool.backend.auth.dto.LoginRequest;
import br.com.alfaschool.backend.auth.dto.LoginResponse;
import br.com.alfaschool.backend.security.JwtService;
import br.com.alfaschool.backend.user.UserAccount;
import br.com.alfaschool.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int MAX_TENTATIVAS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> credenciaisInvalidas());

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo");
        }

        if (isLocked(user)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Conta temporariamente bloqueada por tentativas inválidas");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw credenciaisInvalidas();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName(), user.isSuperAdmin());

        return new LoginResponse(
                token,
                user.getName(),
                user.getEmail(),
                user.isSuperAdmin(),
                "Login realizado com sucesso"
        );
    }

    private boolean isLocked(UserAccount user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void registerFailedAttempt(UserAccount user) {
        int tentativas = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(tentativas);

        if (tentativas >= MAX_TENTATIVAS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            user.setFailedLoginAttempts(0);
        }

        userRepository.save(user);
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Essas credenciais não foram encontradas em nossos registros.");
    }
}
