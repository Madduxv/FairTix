package com.fairtix.payments.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record PaymentIntentRequest(
    @NotEmpty(message = "At least one hold ID is required")
    List<UUID> holdIds) {
}
