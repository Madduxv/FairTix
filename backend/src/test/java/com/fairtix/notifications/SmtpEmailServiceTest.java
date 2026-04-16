package com.fairtix.notifications;

import com.fairtix.notifications.infrastructure.SmtpEmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService = new SmtpEmailService(mailSender, "noreply@fairtix.com");
    }

    @Test
    void sendEmail_callsMailSenderSend() {
        emailService.sendEmail("user@example.com", "Test Subject", "<p>Hello</p>");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_createsNewMimeMessageEachTime() {
        emailService.sendEmail("a@example.com", "S1", "<p>1</p>");
        emailService.sendEmail("b@example.com", "S2", "<p>2</p>");
        verify(mailSender, times(2)).createMimeMessage();
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_wrapsMessagingExceptionAsRuntimeException() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));
        assertThrows(RuntimeException.class,
                () -> emailService.sendEmail("x@example.com", "Subject", "<p>body</p>"));
    }
}
