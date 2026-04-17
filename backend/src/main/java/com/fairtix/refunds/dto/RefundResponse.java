package com.fairtix.refunds.dto;

import com.fairtix.refunds.domain.RefundRequest;
import com.fairtix.refunds.domain.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    UUID orderId,
    UUID userId,
    BigDecimal amount,
    String reason,
    RefundStatus status,
    String adminNotes,
    UUID reviewedBy,
    Instant reviewedAt,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static RefundResponse from(RefundRequest r) {
    return new RefundResponse(
        r.getId(), r.getOrderId(), r.getUserId(),
        r.getAmount(), r.getReason(), r.getStatus(),
        r.getAdminNotes(), r.getReviewedBy(), r.getReviewedAt(),
        r.getCompletedAt(), r.getCreatedAt(), r.getUpdatedAt());
  }
}
