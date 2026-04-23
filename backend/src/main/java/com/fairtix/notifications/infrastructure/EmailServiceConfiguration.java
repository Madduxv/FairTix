package com.fairtix.notifications.infrastructure;

import com.fairtix.notifications.application.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailServiceConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public EmailService smtpEmailService(JavaMailSender mailSender,
                                         @Value("${app.mail.from}") String fromAddress) {
        return new SmtpEmailService(mailSender, fromAddress);
    }

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService noOpEmailService() {
        return new NoOpEmailService();
    }
}
