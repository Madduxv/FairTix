package com.fairtix.fraud.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.fraud.application.RiskScoringService;
import com.fairtix.fraud.application.SuspiciousFlagService;
import com.fairtix.fraud.domain.RiskScore;
import com.fairtix.fraud.domain.SuspiciousFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/fraud")
@PreAuthorize("hasRole('ADMIN')")
public class FraudAdminController {

    private final SuspiciousFlagService suspiciousFlagService;
    private final RiskScoringService riskScoringService;

    public FraudAdminController(SuspiciousFlagService suspiciousFlagService,
                                RiskScoringService riskScoringService) {
        this.suspiciousFlagService = suspiciousFlagService;
        this.riskScoringService = riskScoringService;
    }

    @GetMapping("/flags")
    public Page<FlagResponse> listFlags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return suspiciousFlagService.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(FlagResponse::from);
    }

    @GetMapping("/flags/{userId}")
    public Page<FlagResponse> listFlagsForUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return suspiciousFlagService.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(FlagResponse::from);
    }

    @PatchMapping("/flags/{flagId}/resolve")
    public ResponseEntity<Void> resolveFlag(
            @PathVariable UUID flagId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        suspiciousFlagService.resolve(flagId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/risk/{userId}")
    public ResponseEntity<RiskScoreResponse> getRiskScore(@PathVariable UUID userId) {
        return riskScoringService.getScore(userId)
                .map(rs -> ResponseEntity.ok(RiskScoreResponse.from(rs)))
                .orElse(ResponseEntity.notFound().build());
    }

    public record RiskScoreResponse(
            UUID userId,
            int score,
            String tier,
            int flagCount,
            String notes,
            Instant lastCalculatedAt
    ) {
        static RiskScoreResponse from(RiskScore rs) {
            return new RiskScoreResponse(
                    rs.getUserId(), rs.getScore(), rs.getTier().name(),
                    rs.getFlagCount(), rs.getNotes(), rs.getLastCalculatedAt());
        }
    }

    public record FlagResponse(
            UUID id,
            UUID userId,
            String flagType,
            String severity,
            String details,
            Instant createdAt,
            Instant resolvedAt,
            UUID resolvedBy
    ) {
        static FlagResponse from(SuspiciousFlag f) {
            return new FlagResponse(
                    f.getId(), f.getUserId(), f.getFlagType().name(), f.getSeverity().name(),
                    f.getDetails(), f.getCreatedAt(), f.getResolvedAt(), f.getResolvedBy());
        }
    }
}
