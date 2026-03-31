package com.fairtix.auth;

import com.fairtix.auth.domain.CustomUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Test utility that creates a {@link RequestPostProcessor} setting a
 * {@link CustomUserPrincipal} in the security context — needed because
 * {@code @WithMockUser} produces a standard Spring Security principal
 * which cannot be cast to our custom type.
 */
public final class WithMockPrincipal {

    private WithMockPrincipal() {
    }

    public static RequestPostProcessor user(UUID userId, String email, String role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId, email, "",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    public static RequestPostProcessor user(UUID userId, String email) {
        return user(userId, email, "USER");
    }

    public static RequestPostProcessor admin(UUID userId, String email) {
        return user(userId, email, "ADMIN");
    }
}
