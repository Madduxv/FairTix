package com.fairtix.auth.application;

public class CaptchaRequiredException extends RuntimeException {

  public CaptchaRequiredException() {
    super("reCAPTCHA verification is required.");
  }
}
