package com.fairtix.support.domain;

import com.fairtix.users.domain.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_support_user", columnList = "user_id"),
    @Index(name = "idx_support_status", columnList = "status"),
    @Index(name = "idx_support_assigned", columnList = "assigned_to")
})
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority = TicketPriority.NORMAL;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SupportTicket(User user, String subject, TicketCategory category, UUID orderId, UUID eventId) {
        this.user = user;
        this.subject = subject;
        this.category = category;
        this.orderId = orderId;
        this.eventId = eventId;
        this.status = TicketStatus.OPEN;
        this.priority = TicketPriority.NORMAL;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    protected SupportTicket() {}

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getSubject() { return subject; }
    public TicketCategory getCategory() { return category; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public UUID getOrderId() { return orderId; }
    public UUID getEventId() { return eventId; }
    public UUID getAssignedTo() { return assignedTo; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        if (status == TicketStatus.CLOSED || status == TicketStatus.RESOLVED) {
            this.closedAt = Instant.now();
        }
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
        this.updatedAt = Instant.now();
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
