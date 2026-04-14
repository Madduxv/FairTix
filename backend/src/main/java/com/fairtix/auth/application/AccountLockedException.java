package com.fairtix.auth.application;

public class AccountLockedException extends RuntimeException {

  private final long remainingSeconds;

  public AccountLockedException(long remainingSeconds) {
    super("Too many failed login attempts. Try again in " + remainingSeconds + " seconds.");
    this.remainingSeconds = remainingSeconds;
  }

  public long getRemainingSeconds() {
    return remainingSeconds;
  }
}
