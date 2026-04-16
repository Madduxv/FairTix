package com.fairtix.auth.api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fairtix.auth.application.AuthService;
import com.fairtix.auth.application.JwtService;
import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.users.dto.AuthResponse;
import com.fairtix.users.dto.LoginRequest;
import com.fairtix.users.dto.RegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles user authentication — registration, login, session queries, and logout.
 *
 * <p>Login and register set an HTTP-only cookie containing the JWT.
 * The response body returns user info (no token).
 */
@Tag(name = "Auth", description = "Registration, login, and session management")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String COOKIE_NAME = "fairtix_token";
    private static final int COOKIE_MAX_AGE = 900; // 15 minutes

    private final AuthService service;
    private final JwtService jwtService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Register a new user",
            description = "Creates a user account, sets an HTTP-only auth cookie, and returns user info.")
    @ApiResponse(responseCode = "200", description = "Registration successful")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    @SecurityRequirements
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        String jwt = service.register(request);
        addAuthCookie(response, jwt);
        return buildAuthResponse(jwt);
    }

    @Operation(summary = "Log in",
            description = "Authenticates with email/password, sets an HTTP-only auth cookie, and returns user info.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        String jwt = service.login(request);
        addAuthCookie(response, jwt);
        return buildAuthResponse(jwt);
    }

    @Operation(summary = "Get current user",
            description = "Returns the authenticated user's info based on the auth cookie or bearer token.")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "403", description = "Not authenticated")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        String role = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
        return new AuthResponse(principal.getUserId(), principal.getUsername(), role);
    }

    @Operation(summary = "Log out",
            description = "Clears the HTTP-only auth cookie.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @SecurityRequirements
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void addAuthCookie(HttpServletResponse response, String jwt) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, jwt)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private AuthResponse buildAuthResponse(String jwt) {
        var claims = jwtService.extractAllClaims(jwt);
        return new AuthResponse(
                UUID.fromString(claims.get("userId", String.class)),
                claims.getSubject(),
                claims.get("role", String.class));
    }
}
