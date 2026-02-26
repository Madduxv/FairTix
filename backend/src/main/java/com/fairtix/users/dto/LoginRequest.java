package com.fairtix.users.dto;

public record LoginRequest(
    String email,
    String password) {
}
