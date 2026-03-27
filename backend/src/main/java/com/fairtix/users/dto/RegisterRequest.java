package com.fairtix.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "New user registration payload")
public record RegisterRequest(
        @Schema(description = "Email address for the new account", example = "newuser@example.com")
        String email,
        @Schema(description = "Password (plaintext, will be hashed)", example = "password123")
        String password) {
}
