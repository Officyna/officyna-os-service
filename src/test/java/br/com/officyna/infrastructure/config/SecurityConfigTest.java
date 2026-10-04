package br.com.officyna.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final PasswordEncoderConfig passwordEncoderConfig = new PasswordEncoderConfig();

    @Test
    @DisplayName("Deve gerar e validar hash BCrypt com o encoder do PasswordEncoderConfig")
    void testPasswordEncoder() {
        PasswordEncoder encoder = passwordEncoderConfig.passwordEncoder();
        assertNotNull(encoder);

        String rawPassword = "SenhaSegura@123456";
        String encoded = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, encoded));
    }
}
