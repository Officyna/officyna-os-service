package br.com.officyna.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private PasswordEncoder passwordEncoder;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        userDetailsService = new UserDetailsServiceImpl(passwordEncoder);
    }

    @Test
    @DisplayName("Deve retornar UserDetails para admin@email.com com ROLE_ADMIN")
    void loadUserByUsername_ShouldReturnAdmin() {
        UserDetails details = userDetailsService.loadUserByUsername("admin@email.com");

        assertNotNull(details);
        assertEquals("admin@email.com", details.getUsername());
        assertEquals("encodedPassword", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }

    @Test
    @DisplayName("Deve retornar UserDetails para atendente@email.com com ROLE_ATTENDANT")
    void loadUserByUsername_ShouldReturnAttendant() {
        UserDetails details = userDetailsService.loadUserByUsername("atendente@email.com");

        assertNotNull(details);
        assertEquals("atendente@email.com", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ATTENDANT"::equals));
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando email não existe ou for nulo")
    void loadUserByUsername_ShouldThrowException_WhenEmailNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("naoexiste@email.com"));
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null));
    }
}