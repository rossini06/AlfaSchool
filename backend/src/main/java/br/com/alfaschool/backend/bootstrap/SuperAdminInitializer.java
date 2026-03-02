package br.com.alfaschool.backend.bootstrap;

import br.com.alfaschool.backend.user.UserAccount;
import br.com.alfaschool.backend.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByEmailIgnoreCase("superadmin@alfaschool.com")
                .ifPresentOrElse(
                        user -> {
                        },
                        () -> {
                            UserAccount user = new UserAccount();
                            user.setName("Super Admin");
                            user.setEmail("superadmin@alfaschool.com");
                            user.setPasswordHash(passwordEncoder.encode("SuperAdmin@2024!@#$"));
                            user.setSuperAdmin(true);
                            user.setActive(true);
                            user.setFailedLoginAttempts(0);
                            userRepository.save(user);
                        }
                );
    }
}
