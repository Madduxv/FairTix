package com.fairtix.users.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
