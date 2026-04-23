package com.fairtix.refunds.domain;

public enum RefundStatus {
  REQUESTED,
  PENDING_MANUAL,
  APPROVED,
  COMPLETED,
  REJECTED,
  CANCELLED
}
