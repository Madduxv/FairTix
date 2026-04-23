package com.fairtix.notifications;

import com.fairtix.notifications.application.EmailTemplateService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateServiceTest {

    private final EmailTemplateService templateService = new EmailTemplateService();

    @Test
    void buildVerificationEmail_containsLink() {
        String html = templateService.buildVerificationEmail("Alice", "http://localhost:3000/verify?token=abc");
        assertTrue(html.contains("http://localhost:3000/verify?token=abc"));
        assertTrue(html.contains("Alice"));
    }

    @Test
    void buildVerificationEmail_escapesHtmlInName() {
        String html = templateService.buildVerificationEmail("<script>alert(1)</script>", "http://safe.link");
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void buildPasswordResetEmail_containsLink() {
        String html = templateService.buildPasswordResetEmail("Bob", "http://localhost:3000/reset-password?token=xyz");
        assertTrue(html.contains("http://localhost:3000/reset-password?token=xyz"));
        assertTrue(html.contains("Bob"));
    }

    @Test
    void buildPasswordResetEmail_escapesHtmlInName() {
        String html = templateService.buildPasswordResetEmail("A&B", "http://safe.link");
        assertFalse(html.contains("A&B"));
        assertTrue(html.contains("A&amp;B"));
    }

    @Test
    void buildPasswordResetEmail_escapesHtmlInLink() {
        String html = templateService.buildPasswordResetEmail("Bob", "http://evil.com?x=<img>");
        assertFalse(html.contains("<img>"));
        assertTrue(html.contains("&lt;img&gt;"));
    }
}
