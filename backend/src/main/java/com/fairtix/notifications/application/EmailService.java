package com.fairtix.notifications.application;

public interface EmailService {
    void sendEmail(String to, String subject, String htmlBody);
}
