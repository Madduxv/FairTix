package com.fairtix.support.domain;

import com.fairtix.users.domain.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_messages", indexes = {
    @Index(name = "idx_ticket_msg_ticket", columnList = "ticket_id")
})
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_staff", nullable = false)
    private boolean isStaff;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TicketMessage(SupportTicket ticket, User author, String message, boolean isStaff) {
        this.ticket = ticket;
        this.author = author;
        this.message = message;
        this.isStaff = isStaff;
        this.createdAt = Instant.now();
    }

    protected TicketMessage() {}

    public UUID getId() { return id; }
    public SupportTicket getTicket() { return ticket; }
    public User getAuthor() { return author; }
    public String getMessage() { return message; }
    public boolean isStaff() { return isStaff; }
    public Instant getCreatedAt() { return createdAt; }
}
