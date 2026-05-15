package com.auraia.backend.config;

import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSettings;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.models.enums.Theme;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSettingsRepository;
import com.auraia.backend.services.auth.PasswordPolicyValidator;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "evaluator"})
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Override
    public void run(String... args) {
        if (!properties.getDevDemoUser().isEnabled()) {
            return;
        }
        String email = properties.getDevDemoUser().getEmail().trim().toLowerCase(Locale.ROOT);
        String password = properties.getDevDemoUser().getPassword();
        try {
            passwordPolicyValidator.validate(password);
        } catch (RuntimeException ex) {
            log.warn("DEV_DEMO_USER_PASSWORD does not satisfy backend password policy; demo user skipped");
            return;
        }
        userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name("Maria Solis")
                .role(isAdmin(email) ? Role.ADMIN : Role.USER)
                .plan(Plan.PERSONAL)
                .emailVerified(true)
                .build();
            User saved = userRepository.save(user);
            userSettingsRepository.save(UserSettings.builder()
                .user(saved)
                .theme(Theme.SYSTEM)
                .language("es")
                .timezone("Europe/Madrid")
                .build());
            log.info("Dev demo user seeded: {}", email);
            return saved;
        });
    }

    private boolean isAdmin(String email) {
        return properties.getAdminEmails().stream()
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .anyMatch(email::equals);
    }
}
