package com.fairtix.inventory.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SeatHoldNotFoundException extends RuntimeException {

  public SeatHoldNotFoundException(String message) {
    super(message);
  }
}
