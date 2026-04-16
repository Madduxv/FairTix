package com.fairtix.notifications.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_order", nullable = false)
    private boolean emailOrder = true;

    @Column(name = "email_ticket", nullable = false)
    private boolean emailTicket = true;

    @Column(name = "email_hold", nullable = false)
    private boolean emailHold = false;

    @Column(name = "email_marketing", nullable = false)
    private boolean emailMarketing = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NotificationPreference() {}

    public NotificationPreference(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }

    public boolean isEmailOrder() { return emailOrder; }
    public void setEmailOrder(boolean emailOrder) { this.emailOrder = emailOrder; }

    public boolean isEmailTicket() { return emailTicket; }
    public void setEmailTicket(boolean emailTicket) { this.emailTicket = emailTicket; }

    public boolean isEmailHold() { return emailHold; }
    public void setEmailHold(boolean emailHold) { this.emailHold = emailHold; }

    public boolean isEmailMarketing() { return emailMarketing; }
    public void setEmailMarketing(boolean emailMarketing) { this.emailMarketing = emailMarketing; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
