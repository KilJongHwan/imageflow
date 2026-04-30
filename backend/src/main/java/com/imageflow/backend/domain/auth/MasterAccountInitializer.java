package com.imageflow.backend.domain.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.imageflow.backend.domain.user.AuthProvider;
import com.imageflow.backend.domain.user.User;
import com.imageflow.backend.domain.user.UserPlan;
import com.imageflow.backend.domain.user.UserRepository;
import com.imageflow.backend.domain.user.UserRole;

@Configuration
public class MasterAccountInitializer {

    @Bean
    ApplicationRunner ensureMasterAccount(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.master.email:}") String masterEmail,
            @Value("${app.auth.master.password:}") String masterPassword
    ) {
        return args -> {
            if (masterEmail == null || masterEmail.isBlank() || masterPassword == null || masterPassword.isBlank()) {
                return;
            }

            String normalizedEmail = masterEmail.trim().toLowerCase();
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                return;
            }

            User master = new User(
                    normalizedEmail,
                    passwordEncoder.encode(masterPassword),
                    UserPlan.PRO,
                    999_999,
                    true,
                    UserRole.MASTER,
                    AuthProvider.LOCAL
            );
            userRepository.save(master);
        };
    }
}
