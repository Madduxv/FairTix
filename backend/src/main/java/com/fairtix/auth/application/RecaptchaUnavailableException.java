package com.fairtix.auth.application;

public class RecaptchaUnavailableException extends RuntimeException {

  public RecaptchaUnavailableException(Throwable cause) {
    super("reCAPTCHA service is temporarily unavailable.", cause);
  }
}
