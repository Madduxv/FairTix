package com.fairtix.refunds.application;

public class RefundNotEligibleException extends RuntimeException {
  public RefundNotEligibleException(String message) {
    super(message);
  }
}
