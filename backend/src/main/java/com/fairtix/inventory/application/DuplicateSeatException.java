package com.fairtix.inventory.application;

/**
 * Thrown when a seat with the same event, section, row, and number already exists.
 * Mapped to HTTP 409 by {@link com.fairtix.config.GlobalExceptionHandler}.
 */
public class DuplicateSeatException extends RuntimeException {

  public DuplicateSeatException(String message) {
    super(message);
  }
}
