package com.fairtix.payments.api;

import com.fairtix.payments.dto.PaymentResponse;

public class PaymentProcessingException extends RuntimeException {

  private final PaymentResponse paymentResponse;

  public PaymentProcessingException(PaymentResponse paymentResponse) {
    super("Payment processing failed");
    this.paymentResponse = paymentResponse;
  }

  public PaymentResponse getPaymentResponse() {
    return paymentResponse;
  }
}
