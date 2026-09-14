package com.marcosperboni.payment.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds two demo accounts on first startup so the API can be exercised
 * immediately without a separate provisioning step: admin/admin123 and
 * merchant/merchant123.
 */
@Component
public class DefaultUserSeeder implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("admin", "admin123", Role.ADMIN);
        seedIfMissing("merchant", "merchant123", Role.MERCHANT);
    }

    private void seedIfMissing(String username, String rawPassword, Role role) {
        if (!appUserRepository.existsByUsername(username)) {
            appUserRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), role));
        }
    }
}
