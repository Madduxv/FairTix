package com.fairtix.fraud.infrastructure;

import com.fairtix.fraud.domain.SuspiciousFlag;
import com.fairtix.fraud.domain.SuspiciousFlagSeverity;
import com.fairtix.fraud.domain.SuspiciousFlagType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SuspiciousFlagRepository extends JpaRepository<SuspiciousFlag, UUID> {

    Page<SuspiciousFlag> findByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndFlagTypeAndCreatedAtAfter(UUID userId, SuspiciousFlagType flagType, Instant since);

    boolean existsByUserIdAndSeverityAndResolvedAtIsNull(UUID userId, SuspiciousFlagSeverity severity);

    boolean existsByUserIdAndFlagTypeAndResolvedAtIsNull(UUID userId, SuspiciousFlagType flagType);

    List<SuspiciousFlag> findByUserIdAndResolvedAtIsNull(UUID userId);
}
