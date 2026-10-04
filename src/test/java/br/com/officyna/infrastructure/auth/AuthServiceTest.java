package br.com.officyna.infrastructure.auth;

import br.com.officyna.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expiration", 86_400_000L);
    }

    private UserDetails buildUserDetails(String email, String role) {
        return new org.springframework.security.core.userdetails.User(
                email, "encoded", List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(UserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("Deve autenticar e retornar LoginResponse com sucesso")
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@email.com", "password123");
        UserDetails userDetails = buildUserDetails("user@email.com", "ADMIN");

        when(authenticationManager.authenticate(any())).thenReturn(buildAuthentication(userDetails));
        when(jwtService.generateToken(userDetails)).thenReturn("jwt.token.here");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.token());
        assertEquals("Bearer", response.type());
        assertEquals(86_400_000L, response.expiresIn());
        assertEquals(UserRole.ADMIN, response.role());
        assertEquals("user@email.com", response.userId());
    }

    @Test
    @DisplayName("Deve normalizar o email para minúsculo antes de autenticar")
    void login_ShouldNormalizeEmail() {
        LoginRequest request = new LoginRequest("  USER@EMAIL.COM  ", "password123");
        UserDetails userDetails = buildUserDetails("user@email.com", "ATTENDANT");

        when(authenticationManager.authenticate(any())).thenReturn(buildAuthentication(userDetails));
        when(jwtService.generateToken(userDetails)).thenReturn("jwt.token.here");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(UserRole.ATTENDANT, response.role());
    }

    @Test
    @DisplayName("Deve propagar BadCredentialsException quando autenticação falhar")
    void login_ShouldThrowBadCredentialsException_WhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("user@email.com", "wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
    }
}