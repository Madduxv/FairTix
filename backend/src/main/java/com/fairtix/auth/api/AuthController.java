package com.fairtix.auth.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fairtix.auth.application.AuthService;
import com.fairtix.users.dto.AuthResponse;
import com.fairtix.users.dto.LoginRequest;
import com.fairtix.users.dto.RegisterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Handles user authentication — registration and login.
 *
 * <p>All endpoints are public and return a signed JWT on success.
 */
@Tag(name = "Auth", description = "Registration and login")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /**
     * Registers a new user account.
     *
     * @param request email and password for the new account
     * @return a signed JWT for the newly created user
     */
    @Operation(summary = "Register a new user",
            description = "Creates a user account and returns a JWT token.")
    @ApiResponse(responseCode = "200", description = "Registration successful")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    @SecurityRequirements
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return new AuthResponse(service.register(request));
    }

    /**
     * Authenticates an existing user.
     *
     * @param request email and password credentials
     * @return a signed JWT for the authenticated user
     */
    @Operation(summary = "Log in",
            description = "Authenticates with email/password and returns a JWT token.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return new AuthResponse(service.login(request));
    }
}
