package com.fairtix.auth.application;

import com.fairtix.auth.domain.CustomUserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  private static final String COOKIE_NAME = "fairtix_token";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    // Try cookie first, then fall back to Authorization header
    String token = null;
    if (request.getCookies() != null) {
      for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
        if (COOKIE_NAME.equals(cookie.getName())) {
          token = cookie.getValue();
          break;
        }
      }
    }
    if (token == null) {
      final String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
      }
    }

    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        Claims claims = jwtService.extractAllClaims(token);
        if (claims.getExpiration().after(new java.util.Date())) {
          String email = claims.getSubject();
          UUID userId = UUID.fromString(claims.get("userId", String.class));
          String role = claims.get("role", String.class);

          CustomUserPrincipal principal = new CustomUserPrincipal(
              userId, email, "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));

          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (Exception e) {
        // Malformed, expired, or missing claims — treat as unauthenticated
      }
    }

    filterChain.doFilter(request, response);
  }
}
