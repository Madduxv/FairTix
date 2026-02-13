package com.fairtix.fairtix.inventory.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seat_holds")
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, name = "holder_id")
    private String holderId;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;

    public SeatHold(Seat seat, String holderId, Instant expiresAt) {
        this.seat = seat;
        this.holderId = holderId;
        this.expiresAt = expiresAt;
        this.status = HoldStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    protected SeatHold() {
    }

    public UUID getId() {
        return id;
    }

    public Seat getSeat() {
        return seat;
    }

    public String getHolderId() {
        return holderId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }
}
