package com.fairtix.orders.application;

public class PurchaseCapExceededException extends RuntimeException {
  public PurchaseCapExceededException(String message) {
    super(message);
  }
}
