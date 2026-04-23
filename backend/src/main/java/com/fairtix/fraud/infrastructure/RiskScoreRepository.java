package com.fairtix.fraud.infrastructure;

import com.fairtix.fraud.domain.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {
}
