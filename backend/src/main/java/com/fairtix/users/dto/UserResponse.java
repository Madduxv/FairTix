package com.fairtix.users.dto;

import com.fairtix.users.domain.Role;
import com.fairtix.users.domain.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
