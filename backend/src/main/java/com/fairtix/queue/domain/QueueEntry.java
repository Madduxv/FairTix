package com.fairtix.queue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "admitted_at")
    private Instant admittedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public QueueEntry(UUID eventId, UUID userId, String token, int position) {
        this.eventId = eventId;
        this.userId = userId;
        this.token = token;
        this.position = position;
    }

    protected QueueEntry() {
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public int getPosition() { return position; }
    public QueueStatus getStatus() { return status; }
    public Instant getAdmittedAt() { return admittedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void admit(Instant expiresAt) {
        this.status = QueueStatus.ADMITTED;
        this.admittedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void expire() {
        this.status = QueueStatus.EXPIRED;
    }

    public void complete() {
        this.status = QueueStatus.COMPLETED;
    }
}
