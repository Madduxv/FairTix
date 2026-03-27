package com.fairtix.orders.dto;

import com.fairtix.orders.domain.Order;
import com.fairtix.orders.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Order details")
public record OrderResponse(
        @Schema(description = "Order ID") UUID id,
        @Schema(description = "User ID") UUID userId,
        @Schema(description = "Hold IDs included in this order") List<UUID> holdIds,
        @Schema(description = "Order status") OrderStatus status,
        @Schema(description = "Total amount") BigDecimal totalAmount,
        @Schema(description = "Currency code") String currency,
        @Schema(description = "When the order was created") Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getHoldIds(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt());
    }
}
