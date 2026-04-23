package com.fairtix.support.dto;

import com.fairtix.support.domain.SupportTicket;
import com.fairtix.support.domain.TicketCategory;
import com.fairtix.support.domain.TicketPriority;
import com.fairtix.support.domain.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String subject,
        TicketCategory category,
        TicketStatus status,
        TicketPriority priority,
        UUID orderId,
        UUID eventId,
        UUID assignedTo,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TicketMessageResponse> messages) {

    public static SupportTicketResponse from(SupportTicket t, List<TicketMessageResponse> messages) {
        return new SupportTicketResponse(
                t.getId(),
                t.getUser().getId(),
                t.getUser().getEmail(),
                t.getSubject(),
                t.getCategory(),
                t.getStatus(),
                t.getPriority(),
                t.getOrderId(),
                t.getEventId(),
                t.getAssignedTo(),
                t.getClosedAt(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                messages);
    }

    public static SupportTicketResponse summary(SupportTicket t) {
        return from(t, List.of());
    }
}
