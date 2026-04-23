package com.fairtix.fraud.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.fraud.domain.RiskTier;
import com.fairtix.fraud.domain.SuspiciousFlagType;
import com.fairtix.fraud.infrastructure.SuspiciousFlagRepository;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class StepUpGateService {

    private static final String REDIS_KEY_PREFIX = "step_up_verified:";

    private final RiskScoringService riskScoringService;
    private final SuspiciousFlagRepository flagRepository;
    private final RedissonClient redissonClient;
    private final AuditService auditService;

    @Value("${fraud.stepup.verified-ttl-minutes:15}")
    private int verifiedTtlMinutes;

    @Value("${fraud.stepup.high-tier-checkout-enabled:true}")
    private boolean highTierCheckoutEnabled;

    @Value("${fraud.stepup.critical-any-action-enabled:true}")
    private boolean criticalAnyActionEnabled;

    @Value("${fraud.stepup.rapid-hold-cycling-enabled:true}")
    private boolean rapidHoldCyclingEnabled;

    public StepUpGateService(RiskScoringService riskScoringService,
                             SuspiciousFlagRepository flagRepository,
                             RedissonClient redissonClient,
                             AuditService auditService) {
        this.riskScoringService = riskScoringService;
        this.flagRepository = flagRepository;
        this.redissonClient = redissonClient;
        this.auditService = auditService;
    }

    public boolean requiresStepUp(UUID userId, String action) {
        RiskTier tier = riskScoringService.getTier(userId);

        if (criticalAnyActionEnabled && tier == RiskTier.CRITICAL) {
            return true;
        }

        if (highTierCheckoutEnabled && tier == RiskTier.HIGH && "CHECKOUT".equals(action)) {
            return true;
        }

        if (rapidHoldCyclingEnabled && "SEAT_HOLD".equals(action)) {
            return flagRepository.existsByUserIdAndFlagTypeAndResolvedAtIsNull(
                    userId, SuspiciousFlagType.RAPID_HOLD_CYCLING);
        }

        return false;
    }

    public void markVerified(UUID userId) {
        RBucket<Boolean> bucket = redissonClient.getBucket(REDIS_KEY_PREFIX + userId);
        bucket.set(Boolean.TRUE, Duration.ofMinutes(verifiedTtlMinutes));
        auditService.log(userId, "STEP_UP_VERIFIED", "FRAUD", userId, "ttl_minutes=" + verifiedTtlMinutes);
    }

    public boolean isVerified(UUID userId) {
        RBucket<Boolean> bucket = redissonClient.getBucket(REDIS_KEY_PREFIX + userId);
        return Boolean.TRUE.equals(bucket.get());
    }
}
