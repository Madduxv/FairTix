package com.fairtix.tickets.dto;

import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.domain.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Ticket details")
public record TicketResponse(
        @Schema(description = "Ticket ID") UUID id,
        @Schema(description = "Order ID") UUID orderId,
        @Schema(description = "Event ID") UUID eventId,
        @Schema(description = "Event title") String eventTitle,
        @Schema(description = "Event venue") String eventVenue,
        @Schema(description = "Event start time") Instant eventStartTime,
        @Schema(description = "Seat ID") UUID seatId,
        @Schema(description = "Seat section") String seatSection,
        @Schema(description = "Seat row") String seatRow,
        @Schema(description = "Seat number") String seatNumber,
        @Schema(description = "Ticket price") BigDecimal price,
        @Schema(description = "Ticket holder email") String holderEmail,
        @Schema(description = "Ticket status") TicketStatus status,
        @Schema(description = "When the ticket was issued") Instant issuedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getOrder().getId(),
                ticket.getEvent().getId(),
                ticket.getEvent().getTitle(),
                ticket.getEvent().getVenue(),
                ticket.getEvent().getStartTime(),
                ticket.getSeat().getId(),
                ticket.getSeat().getSection(),
                ticket.getSeat().getRowLabel(),
                ticket.getSeat().getSeatNumber(),
                ticket.getPrice(),
                ticket.getUser().getEmail(),
                ticket.getStatus(),
                ticket.getIssuedAt());
    }
}
