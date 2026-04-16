package com.fairtix.queue.api;

import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;

import java.time.Instant;
import java.util.UUID;

public record QueueStatusResponse(
        UUID id,
        int position,
        QueueStatus status,
        long totalAhead,
        Instant expiresAt,
        Instant admittedAt) {

    public static QueueStatusResponse from(QueueEntry entry, long totalAhead) {
        return new QueueStatusResponse(
                entry.getId(),
                entry.getPosition(),
                entry.getStatus(),
                totalAhead,
                entry.getExpiresAt(),
                entry.getAdmittedAt());
    }
}
