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

    public String buildTransferRequestEmail(String recipientName, String senderEmail,
                                            String eventTitle, String seat, String acceptUrl) {
        String safeName = htmlEscape(recipientName);
        String safeSender = htmlEscape(senderEmail);
        String safeEvent = htmlEscape(eventTitle);
        String safeSeat = htmlEscape(seat);
        return "<html><body style=\"font-family:sans-serif;\">"
                + "<h2>You have a ticket transfer request on FairTix</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p><strong>" + safeSender + "</strong> wants to transfer their ticket to you.</p>"
                + "<p><strong>Event:</strong> " + safeEvent + "<br>"
                + "<strong>Seat:</strong> " + safeSeat + "</p>"
                + "<p>Log in to FairTix and visit <em>My Tickets &rarr; Transfer Requests</em> to accept or reject this request. "
                + "This request expires in 7 days.</p>"
                + "</body></html>";
    }

    public String buildTransferAcceptedEmail(String senderName, String recipientEmail,
                                             String eventTitle, String seat) {
        String safeName = htmlEscape(senderName);
        String safeRecipient = htmlEscape(recipientEmail);
        String safeEvent = htmlEscape(eventTitle);
        String safeSeat = htmlEscape(seat);
        return "<html><body style=\"font-family:sans-serif;\">"
                + "<h2>Your ticket transfer was accepted</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p><strong>" + safeRecipient + "</strong> accepted your transfer for:</p>"
                + "<p><strong>Event:</strong> " + safeEvent + "<br>"
                + "<strong>Seat:</strong> " + safeSeat + "</p>"
                + "</body></html>";
    }

    public String buildTransferRejectedEmail(String senderName, String recipientEmail,
                                             String eventTitle, String seat) {
        String safeName = htmlEscape(senderName);
        String safeRecipient = htmlEscape(recipientEmail);
        String safeEvent = htmlEscape(eventTitle);
        String safeSeat = htmlEscape(seat);
        return "<html><body style=\"font-family:sans-serif;\">"
                + "<h2>Your ticket transfer was declined</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p><strong>" + safeRecipient + "</strong> declined your transfer for:</p>"
                + "<p><strong>Event:</strong> " + safeEvent + "<br>"
                + "<strong>Seat:</strong> " + safeSeat + "</p>"
                + "<p>Your ticket is still valid and has been returned to your account.</p>"
                + "</body></html>";
    }

    public String buildTransferExpiredEmail(String senderName, String eventTitle, String seat) {
        String safeName = htmlEscape(senderName);
        String safeEvent = htmlEscape(eventTitle);
        String safeSeat = htmlEscape(seat);
        return "<html><body style=\"font-family:sans-serif;\">"
                + "<h2>Your ticket transfer request has expired</h2>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Your transfer request for the following ticket was not accepted in time:</p>"
                + "<p><strong>Event:</strong> " + safeEvent + "<br>"
                + "<strong>Seat:</strong> " + safeSeat + "</p>"
                + "<p>Your ticket is still valid and remains in your account.</p>"
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
