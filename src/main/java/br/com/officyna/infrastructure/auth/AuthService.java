package br.com.officyna.infrastructure.auth;

import br.com.officyna.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private long expiration;

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        UserRole role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(r -> {
                    try {
                        return UserRole.valueOf(r);
                    } catch (Exception e) {
                        return UserRole.ATTENDANT;
                    }
                })
                .orElse(UserRole.ATTENDANT);

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(expiration)
                .userId(normalizedEmail)
                .name(normalizedEmail)
                .role(role)
                .build();
    }

    private static @NonNull String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}