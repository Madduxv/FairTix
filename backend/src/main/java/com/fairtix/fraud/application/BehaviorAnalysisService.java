package com.fairtix.fraud.application;

import com.fairtix.audit.infrastructure.AuditLogRepository;
import com.fairtix.fraud.domain.SuspiciousFlagSeverity;
import com.fairtix.fraud.domain.SuspiciousFlagType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class BehaviorAnalysisService {

    private final AuditLogRepository auditLogRepository;
    private final SuspiciousFlagService suspiciousFlagService;

    @Value("${fraud.rules.rapid-hold-count:5}")
    private int rapidHoldCount;

    @Value("${fraud.rules.rapid-hold-window-minutes:2}")
    private int rapidHoldWindowMinutes;

    @Value("${fraud.rules.volume-anomaly-count:10}")
    private int volumeAnomalyCount;

    @Value("${fraud.rules.volume-anomaly-window-minutes:10}")
    private int volumeAnomalyWindowMinutes;

    @Value("${fraud.rules.high-release-rate-threshold:0.8}")
    private double highReleaseRateThreshold;

    @Value("${fraud.rules.high-release-rate-window-minutes:60}")
    private int highReleaseRateWindowMinutes;

    @Value("${fraud.rules.failed-payment-count:3}")
    private int failedPaymentCount;

    @Value("${fraud.rules.failed-payment-window-hours:24}")
    private int failedPaymentWindowHours;

    public BehaviorAnalysisService(AuditLogRepository auditLogRepository,
                                   SuspiciousFlagService suspiciousFlagService) {
        this.auditLogRepository = auditLogRepository;
        this.suspiciousFlagService = suspiciousFlagService;
    }

    public void analyzeUser(UUID userId) {
        checkRapidHoldCycling(userId);
        checkVolumeAnomaly(userId);
        checkHighReleaseRate(userId);
        checkRepeatedFailedPayments(userId);
    }

    private void checkRapidHoldCycling(UUID userId) {
        Instant since = Instant.now().minus(rapidHoldWindowMinutes, ChronoUnit.MINUTES);
        long holdCount = auditLogRepository.countByUserIdAndActionAndResourceTypeAndCreatedAtAfter(
                userId, "CREATE", "HOLD", since);
        if (holdCount > rapidHoldCount) {
            suspiciousFlagService.flag(userId, SuspiciousFlagType.RAPID_HOLD_CYCLING,
                    SuspiciousFlagSeverity.HIGH,
                    holdCount + " holds in " + rapidHoldWindowMinutes + " minutes");
        }
    }

    private void checkVolumeAnomaly(UUID userId) {
        Instant since = Instant.now().minus(volumeAnomalyWindowMinutes, ChronoUnit.MINUTES);
        long holdCount = auditLogRepository.countByUserIdAndActionAndResourceTypeAndCreatedAtAfter(
                userId, "CREATE", "HOLD", since);
        if (holdCount > volumeAnomalyCount) {
            suspiciousFlagService.flag(userId, SuspiciousFlagType.VOLUME_ANOMALY,
                    SuspiciousFlagSeverity.MEDIUM,
                    holdCount + " hold attempts in " + volumeAnomalyWindowMinutes + " minutes");
        }
    }

    private void checkHighReleaseRate(UUID userId) {
        Instant since = Instant.now().minus(highReleaseRateWindowMinutes, ChronoUnit.MINUTES);
        long created = auditLogRepository.countByUserIdAndActionAndResourceTypeAndCreatedAtAfter(
                userId, "CREATE", "HOLD", since);
        if (created < 5) {
            // Not enough data to determine a meaningful rate
            return;
        }
        long released = auditLogRepository.countByUserIdAndActionAndResourceTypeAndCreatedAtAfter(
                userId, "RELEASE", "HOLD", since);
        if (released > 0 && (double) released / created > highReleaseRateThreshold) {
            suspiciousFlagService.flag(userId, SuspiciousFlagType.HIGH_RELEASE_RATE,
                    SuspiciousFlagSeverity.MEDIUM,
                    released + "/" + created + " holds released");
        }
    }

    private void checkRepeatedFailedPayments(UUID userId) {
        Instant since = Instant.now().minus(failedPaymentWindowHours, ChronoUnit.HOURS);
        long failures = auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                userId, "PAYMENT_FAILED", since);
        if (failures > failedPaymentCount) {
            suspiciousFlagService.flag(userId, SuspiciousFlagType.REPEATED_FAILED_PAYMENTS,
                    SuspiciousFlagSeverity.HIGH,
                    failures + " payment failures in " + failedPaymentWindowHours + " hours");
        }
    }
}
