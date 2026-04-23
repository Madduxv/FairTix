package com.fairtix.fraud.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.fraud.domain.SuspiciousFlag;
import com.fairtix.fraud.domain.SuspiciousFlagSeverity;
import com.fairtix.fraud.domain.SuspiciousFlagType;
import com.fairtix.fraud.infrastructure.SuspiciousFlagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SuspiciousFlagService {

    private final SuspiciousFlagRepository flagRepository;
    private final AuditService auditService;
    private final RiskScoringService riskScoringService;

    @Value("${fraud.flag.dedup-window-minutes:30}")
    private int dedupWindowMinutes;

    public SuspiciousFlagService(SuspiciousFlagRepository flagRepository,
                                 AuditService auditService,
                                 @Lazy RiskScoringService riskScoringService) {
        this.flagRepository = flagRepository;
        this.auditService = auditService;
        this.riskScoringService = riskScoringService;
    }

    @Transactional
    public void flag(UUID userId, SuspiciousFlagType type, SuspiciousFlagSeverity severity, String details) {
        Instant dedupSince = Instant.now().minus(dedupWindowMinutes, ChronoUnit.MINUTES);
        if (flagRepository.existsByUserIdAndFlagTypeAndCreatedAtAfter(userId, type, dedupSince)) {
            return;
        }
        SuspiciousFlag flag = new SuspiciousFlag(userId, type, severity, details);
        flagRepository.save(flag);
        auditService.log(userId, "FRAUD_FLAG_CREATED", "FRAUD", userId,
                "type=" + type.name() + " severity=" + severity.name());
        riskScoringService.recalculate(userId);
    }

    public boolean hasActiveCriticalFlag(UUID userId) {
        return flagRepository.existsByUserIdAndSeverityAndResolvedAtIsNull(userId, SuspiciousFlagSeverity.HIGH);
    }

    @Transactional
    public void resolve(UUID flagId, UUID resolvedBy) {
        SuspiciousFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Flag not found: " + flagId));
        flag.resolve(resolvedBy);
        flagRepository.save(flag);
        auditService.log(resolvedBy, "FRAUD_FLAG_RESOLVED", "FRAUD", flagId,
                "userId=" + flag.getUserId() + " type=" + flag.getFlagType().name());
        riskScoringService.recalculate(flag.getUserId());
    }

    public Page<SuspiciousFlag> findAll(Pageable pageable) {
        return flagRepository.findAll(pageable);
    }

    public Page<SuspiciousFlag> findByUserId(UUID userId, Pageable pageable) {
        return flagRepository.findByUserId(userId, pageable);
    }
}
