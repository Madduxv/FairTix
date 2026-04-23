package com.fairtix.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suspicious_flags")
public class SuspiciousFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SuspiciousFlagType flagType;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuspiciousFlagSeverity severity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant resolvedAt;

    private UUID resolvedBy;

    protected SuspiciousFlag() {
    }

    public SuspiciousFlag(UUID userId, SuspiciousFlagType flagType, SuspiciousFlagSeverity severity, String details) {
        this.userId = userId;
        this.flagType = flagType;
        this.severity = severity;
        this.details = details;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public SuspiciousFlagType getFlagType() { return flagType; }
    public String getDetails() { return details; }
    public SuspiciousFlagSeverity getSeverity() { return severity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public UUID getResolvedBy() { return resolvedBy; }

    public void resolve(UUID resolvedBy) {
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }
}
