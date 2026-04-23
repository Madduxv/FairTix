package com.fairtix.queue.infrastructure;

import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueRepository extends JpaRepository<QueueEntry, UUID> {

    Optional<QueueEntry> findByEventIdAndUserId(UUID eventId, UUID userId);

    Optional<QueueEntry> findByToken(String token);

    long countByEventIdAndStatus(UUID eventId, QueueStatus status);

    List<QueueEntry> findByEventIdAndStatusOrderByPositionAsc(UUID eventId, QueueStatus status, Pageable pageable);

    @Query("SELECT DISTINCT q.eventId FROM QueueEntry q WHERE q.status = :status")
    List<UUID> findDistinctEventIdsByStatus(@Param("status") QueueStatus status);

    @Query("SELECT DISTINCT q.eventId FROM QueueEntry q WHERE q.status = :status AND q.expiresAt < :now")
    List<UUID> findDistinctEventIdsWithExpiredByStatus(
            @Param("status") QueueStatus status,
            @Param("now") Instant now);

    List<QueueEntry> findByStatusAndExpiresAtBefore(QueueStatus status, Instant now);
}
