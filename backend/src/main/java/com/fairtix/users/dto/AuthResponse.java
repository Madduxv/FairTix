package com.fairtix.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing a JWT token")
public record AuthResponse(
        @Schema(description = "JWT bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token) {
}
