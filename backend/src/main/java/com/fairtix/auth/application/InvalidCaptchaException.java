package com.fairtix.auth.application;

public class InvalidCaptchaException extends RuntimeException {

  public InvalidCaptchaException() {
    super("reCAPTCHA verification failed.");
  }
}
