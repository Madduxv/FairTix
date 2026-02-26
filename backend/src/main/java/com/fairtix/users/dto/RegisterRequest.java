package com.fairtix.users.dto;

public record RegisterRequest(
    String email,
    String password) {
}
