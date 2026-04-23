package com.fairtix.payments.application;

public class PaymentDeclinedException extends RuntimeException {

  public PaymentDeclinedException(String message) {
    super(message);
  }
}
