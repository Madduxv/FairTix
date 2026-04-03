package com.fairtix.payments.application;

import com.fairtix.payments.domain.PaymentStatus;

public class PaymentFailedException extends RuntimeException {

  private final PaymentStatus status;
  private final String transactionId;

  public PaymentFailedException(String message, PaymentStatus status, String transactionId) {
    super(message);
    this.status = status;
    this.transactionId = transactionId;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getTransactionId() {
    return transactionId;
  }
}
