package com.fairtix.tickets.domain;

import com.fairtix.users.domain.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_transfer_requests")
public class TicketTransferRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "from_user_id", nullable = false)
  private User fromUser;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "to_user_id", nullable = false)
  private User toUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransferStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  public TicketTransferRequest(Ticket ticket, User fromUser, User toUser, Instant expiresAt) {
    this.ticket = ticket;
    this.fromUser = fromUser;
    this.toUser = toUser;
    this.status = TransferStatus.PENDING;
    this.createdAt = Instant.now();
    this.expiresAt = expiresAt;
  }

  protected TicketTransferRequest() {}

  public UUID getId() { return id; }
  public Ticket getTicket() { return ticket; }
  public User getFromUser() { return fromUser; }
  public User getToUser() { return toUser; }
  public TransferStatus getStatus() { return status; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getResolvedAt() { return resolvedAt; }

  public void setStatus(TransferStatus status) { this.status = status; }
  public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
