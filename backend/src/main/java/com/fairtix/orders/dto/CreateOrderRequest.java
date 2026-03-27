package com.fairtix.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Payload for creating an order from confirmed holds")
public record CreateOrderRequest(

        @Schema(description = "IDs of confirmed holds to include in this order",
                example = "[\"c3d4e5f6-a7b8-9012-cdef-123456789012\"]")
        @NotEmpty(message = "At least one hold ID is required")
        List<UUID> holdIds) {
}
