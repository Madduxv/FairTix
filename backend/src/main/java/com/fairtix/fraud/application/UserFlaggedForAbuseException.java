package com.fairtix.fraud.application;

public class UserFlaggedForAbuseException extends RuntimeException {
    public UserFlaggedForAbuseException() {
        super("Your account has been flagged for suspicious activity. Please contact support.");
    }
}
