package com.fairtix.notifications.application;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildVerificationEmail(String name, String verificationLink) {
        String safeName = htmlEscape(name);
        String safeLink = htmlEscape(verificationLink);
        return "<html><body style=\"font-family:sans-serif;\">"
                + "<h2>Verify your FairTix account</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Click the link below to verify your email address. This link expires in 24 hours.</p>"
                + "<p><a href=\"" + safeLink + "\">" + safeLink + "</a></p>"
                + "<p>If you did not create a FairTix account, you can safely ignore this email.</p>"
                + "</body></html>";
    }

    public String buildPasswordResetEmail(String name, String resetLink) {
        String safeName = htmlEscape(name);
        String safeLink = htmlEscape(resetLink);
        return "<html><body style=\"font-family:sans-serif;\">"
                + "<h2>Reset your FairTix password</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Click the link below to reset your password. This link expires in 1 hour.</p>"
                + "<p><a href=\"" + safeLink + "\">" + safeLink + "</a></p>"
                + "<p>If you did not request a password reset, you can safely ignore this email.</p>"
                + "</body></html>";
    }

    private String htmlEscape(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
