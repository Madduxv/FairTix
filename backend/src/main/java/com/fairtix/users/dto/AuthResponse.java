package com.fairtix.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Authentication response containing the current user's info")
public record AuthResponse(
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "User email") String email,
        @Schema(description = "User role") String role,
        @Schema(description = "Whether the user's email address has been verified") boolean emailVerified) {
}
