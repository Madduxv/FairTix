package com.fairtix.tickets.dto;

import com.fairtix.tickets.domain.TicketTransferRequest;
import com.fairtix.tickets.domain.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record TransferRequestResponse(
        UUID id,
        UUID ticketId,
        String eventTitle,
        String seatSection,
        String seatRow,
        String seatNumber,
        String fromEmail,
        String toEmail,
        TransferStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant resolvedAt) {

    public static TransferRequestResponse from(TicketTransferRequest r) {
        return new TransferRequestResponse(
                r.getId(),
                r.getTicket().getId(),
                r.getTicket().getEvent().getTitle(),
                r.getTicket().getSeat().getSection(),
                r.getTicket().getSeat().getRowLabel(),
                r.getTicket().getSeat().getSeatNumber(),
                r.getFromUser().getEmail(),
                r.getToUser().getEmail(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getExpiresAt(),
                r.getResolvedAt());
    }
}
