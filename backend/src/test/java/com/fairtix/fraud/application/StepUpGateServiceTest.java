package com.fairtix.fraud.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.fraud.domain.RiskTier;
import com.fairtix.fraud.domain.SuspiciousFlagType;
import com.fairtix.fraud.infrastructure.SuspiciousFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StepUpGateServiceTest {

    @Mock private RiskScoringService riskScoringService;
    @Mock private SuspiciousFlagRepository flagRepository;
    @Mock private RedissonClient redissonClient;
    @Mock private AuditService auditService;
    @InjectMocks private StepUpGateService stepUpGateService;

    @SuppressWarnings("unchecked")
    private final RBucket<Boolean> bucket = mock(RBucket.class);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stepUpGateService, "verifiedTtlMinutes", 15);
        ReflectionTestUtils.setField(stepUpGateService, "highTierCheckoutEnabled", true);
        ReflectionTestUtils.setField(stepUpGateService, "criticalAnyActionEnabled", true);
        ReflectionTestUtils.setField(stepUpGateService, "rapidHoldCyclingEnabled", true);
        doReturn(bucket).when(redissonClient).getBucket(anyString());
    }

    @Test
    void requiresStepUpForCriticalTierOnAnyAction() {
        when(riskScoringService.getTier(userId)).thenReturn(RiskTier.CRITICAL);

        assertThat(stepUpGateService.requiresStepUp(userId, "SOME_ACTION")).isTrue();
    }

    @Test
    void requiresStepUpForHighTierAtCheckout() {
        when(riskScoringService.getTier(userId)).thenReturn(RiskTier.HIGH);

        assertThat(stepUpGateService.requiresStepUp(userId, "CHECKOUT")).isTrue();
    }

    @Test
    void doesNotRequireStepUpForHighTierOnNonCheckoutAction() {
        when(riskScoringService.getTier(userId)).thenReturn(RiskTier.HIGH);
        when(flagRepository.existsByUserIdAndFlagTypeAndResolvedAtIsNull(userId,
                SuspiciousFlagType.RAPID_HOLD_CYCLING)).thenReturn(false);

        assertThat(stepUpGateService.requiresStepUp(userId, "OTHER_ACTION")).isFalse();
    }

    @Test
    void requiresStepUpForSeatHoldWhenRapidHoldCyclingFlagActive() {
        when(riskScoringService.getTier(userId)).thenReturn(RiskTier.MEDIUM);
        when(flagRepository.existsByUserIdAndFlagTypeAndResolvedAtIsNull(userId,
                SuspiciousFlagType.RAPID_HOLD_CYCLING)).thenReturn(true);

        assertThat(stepUpGateService.requiresStepUp(userId, "SEAT_HOLD")).isTrue();
    }

    @Test
    void doesNotRequireStepUpForLowTier() {
        when(riskScoringService.getTier(userId)).thenReturn(RiskTier.LOW);
        when(flagRepository.existsByUserIdAndFlagTypeAndResolvedAtIsNull(userId,
                SuspiciousFlagType.RAPID_HOLD_CYCLING)).thenReturn(false);

        assertThat(stepUpGateService.requiresStepUp(userId, "SEAT_HOLD")).isFalse();
        assertThat(stepUpGateService.requiresStepUp(userId, "CHECKOUT")).isFalse();
    }

    @Test
    void doesNotRequireStepUpForMediumTierWithoutRapidHoldFlag() {
        when(riskScoringService.getTier(userId)).thenReturn(RiskTier.MEDIUM);
        when(flagRepository.existsByUserIdAndFlagTypeAndResolvedAtIsNull(userId,
                SuspiciousFlagType.RAPID_HOLD_CYCLING)).thenReturn(false);

        assertThat(stepUpGateService.requiresStepUp(userId, "SEAT_HOLD")).isFalse();
        assertThat(stepUpGateService.requiresStepUp(userId, "CHECKOUT")).isFalse();
    }

    @Test
    void markVerifiedStoresTrueInRedis() {
        stepUpGateService.markVerified(userId);

        verify(bucket).set(Boolean.TRUE, Duration.ofMinutes(15));
    }

    @Test
    void isVerifiedReturnsTrueWhenBucketHoldsTrue() {
        when(bucket.get()).thenReturn(Boolean.TRUE);

        assertThat(stepUpGateService.isVerified(userId)).isTrue();
    }

    @Test
    void isVerifiedReturnsFalseWhenBucketIsEmpty() {
        when(bucket.get()).thenReturn(null);

        assertThat(stepUpGateService.isVerified(userId)).isFalse();
    }
}
