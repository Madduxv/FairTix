package com.fairtix.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_risk_scores")
public class RiskScore {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskTier tier;

    @Column(nullable = false)
    private int flagCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Instant lastCalculatedAt;

    protected RiskScore() {}

    public RiskScore(UUID userId, int score, RiskTier tier, int flagCount, String notes) {
        this.userId = userId;
        this.score = score;
        this.tier = tier;
        this.flagCount = flagCount;
        this.notes = notes;
        this.lastCalculatedAt = Instant.now();
    }

    public void update(int score, RiskTier tier, int flagCount, String notes) {
        this.score = score;
        this.tier = tier;
        this.flagCount = flagCount;
        this.notes = notes;
        this.lastCalculatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public int getScore() { return score; }
    public RiskTier getTier() { return tier; }
    public int getFlagCount() { return flagCount; }
    public String getNotes() { return notes; }
    public Instant getLastCalculatedAt() { return lastCalculatedAt; }
}
