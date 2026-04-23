package com.fairtix.orders.dto;

import com.fairtix.orders.domain.Order;
import com.fairtix.orders.domain.OrderStatus;
import com.fairtix.tickets.domain.Ticket;
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
        @Schema(description = "When the order was created") Instant createdAt,
        @Schema(description = "Event title") String eventTitle,
        @Schema(description = "Event ID") UUID eventId,
        @Schema(description = "Event start time") Instant eventStartTime,
        @Schema(description = "Number of tickets") int ticketCount,
        @Schema(description = "Ticket summaries") List<TicketSummary> tickets) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getHoldIds(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                null, null, null, 0, List.of());
    }

    public static OrderResponse withDetails(Order order, List<Ticket> tickets) {
        String eventTitle = null;
        UUID eventId = null;
        Instant eventStartTime = null;
        if (!tickets.isEmpty()) {
            var event = tickets.get(0).getEvent();
            eventTitle = event.getTitle();
            eventId = event.getId();
            eventStartTime = event.getStartTime();
        }
        List<TicketSummary> summaries = tickets.stream()
                .map(t -> new TicketSummary(
                        t.getId(),
                        t.getSeat().getSection(),
                        t.getSeat().getRowLabel(),
                        t.getSeat().getSeatNumber()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getHoldIds(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                eventTitle, eventId, eventStartTime,
                tickets.size(), summaries);
    }
}
