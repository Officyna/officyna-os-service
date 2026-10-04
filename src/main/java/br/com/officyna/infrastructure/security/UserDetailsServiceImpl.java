package br.com.officyna.infrastructure.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final Map<String, UserDetails> inMemoryUsers = new ConcurrentHashMap<>();

    // TODO: Substituir pela consulta ao serviço de usuarios
    public UserDetailsServiceImpl(PasswordEncoder passwordEncoder) {
        inMemoryUsers.put("admin@email.com", new User(
                "admin@email.com",
                passwordEncoder.encode("admin123"),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        inMemoryUsers.put("atendente@email.com", new User(
                "atendente@email.com",
                passwordEncoder.encode("atendente123"),
                List.of(new SimpleGrantedAuthority("ROLE_ATTENDANT"))));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) {
            throw new UsernameNotFoundException("Email cannot be null");
        }
        UserDetails user = inMemoryUsers.get(email.toLowerCase(Locale.ROOT).trim());
        if (user != null) {
            return user;
        }
        throw new UsernameNotFoundException("Usuário não encontrado: " + email);
    }
}