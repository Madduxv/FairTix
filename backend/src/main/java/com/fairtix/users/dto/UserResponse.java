package com.fairtix.users.dto;

import com.fairtix.users.domain.Role;
import com.fairtix.users.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "User details")
public record UserResponse(
        @Schema(description = "User ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Email address", example = "user@example.com")
        String email,
        @Schema(description = "User role", example = "USER")
        Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
