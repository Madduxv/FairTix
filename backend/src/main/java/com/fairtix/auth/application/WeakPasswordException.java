package com.fairtix.auth.application;

public class WeakPasswordException extends RuntimeException {

  public WeakPasswordException(String message) {
    super(message);
  }
}
