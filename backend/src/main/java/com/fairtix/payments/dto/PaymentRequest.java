package com.fairtix.payments.dto;

import com.fairtix.payments.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Payment request — simulation mode or Stripe mode")
public record PaymentRequest(

    @Schema(description = "IDs of confirmed holds to pay for",
        example = "[\"c3d4e5f6-a7b8-9012-cdef-123456789012\"]")
    @NotEmpty(message = "At least one hold ID is required")
    List<UUID> holdIds,

    @Schema(description = "Simulated payment outcome (SUCCESS, FAILURE, CANCELLED). "
        + "Omit or set to null for random outcome. Ignored when Stripe is enabled.",
        example = "SUCCESS")
    PaymentStatus simulatedOutcome,

    @Schema(description = "Stripe PaymentIntent ID. Required when Stripe is enabled.",
        example = "pi_3...")
    String paymentIntentId) {
}
