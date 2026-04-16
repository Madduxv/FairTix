package com.fairtix.auth.scheduler;

import com.fairtix.auth.infrastructure.EmailVerificationTokenRepository;
import com.fairtix.auth.infrastructure.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class VerificationTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(VerificationTokenCleanupScheduler.class);

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public VerificationTokenCleanupScheduler(
            EmailVerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Scheduled(fixedDelayString = "${verification.token.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanUpExpiredTokens() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        verificationTokenRepository.deleteExpiredBefore(cutoff);
        passwordResetTokenRepository.deleteExpiredBefore(cutoff);
        log.debug("Cleaned up expired verification and password reset tokens older than 1h past expiry");
    }
}
