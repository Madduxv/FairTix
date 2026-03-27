package com.fairtix.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "User email address", example = "user@example.com")
        String email,
        @Schema(description = "User password", example = "password123")
        String password) {
}
