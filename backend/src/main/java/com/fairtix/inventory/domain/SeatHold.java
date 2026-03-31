package com.fairtix.inventory.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seat_holds", indexes = {
    @Index(name = "idx_hold_status_expires", columnList = "status, expires_at"),
    @Index(name = "idx_hold_owner_id",       columnList = "owner_id")
})
public class SeatHold {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "seat_id", nullable = false)
  private Seat seat;

  @Column(nullable = false, name = "owner_id")
  private UUID ownerId;

  @Column(nullable = false, name = "expires_at")
  private Instant expiresAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HoldStatus status;

  @Column(nullable = false, name = "created_at", updatable = false)
  private Instant createdAt;

  public SeatHold(Seat seat, UUID ownerId, Instant expiresAt) {
    this.seat = seat;
    this.ownerId = ownerId;
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

  public UUID getOwnerId() {
    return ownerId;
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
