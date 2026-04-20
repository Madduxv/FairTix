package com.fairtix.support.dto;

import com.fairtix.support.domain.TicketMessage;

import java.time.Instant;
import java.util.UUID;

public record TicketMessageResponse(
        UUID id,
        UUID authorId,
        String authorEmail,
        String message,
        boolean isStaff,
        Instant createdAt) {

    public static TicketMessageResponse from(TicketMessage m) {
        return new TicketMessageResponse(
                m.getId(),
                m.getAuthor().getId(),
                m.getAuthor().getEmail(),
                m.getMessage(),
                m.isStaff(),
                m.getCreatedAt());
    }
}
