package com.fairtix.queue.api;

import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;

import java.time.Instant;
import java.util.UUID;

public record JoinQueueResponse(
        UUID id,
        String token,
        int position,
        QueueStatus status,
        long totalAhead,
        Instant expiresAt,
        Instant createdAt) {

    public static JoinQueueResponse from(QueueEntry entry, long totalAhead) {
        return new JoinQueueResponse(
                entry.getId(),
                entry.getToken(),
                entry.getPosition(),
                entry.getStatus(),
                totalAhead,
                entry.getExpiresAt(),
                entry.getCreatedAt());
    }
}
