package com.fairtix.fraud.domain;

public enum RiskTier {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskTier fromScore(int score) {
        if (score >= 75) return CRITICAL;
        if (score >= 50) return HIGH;
        if (score >= 25) return MEDIUM;
        return LOW;
    }
}
