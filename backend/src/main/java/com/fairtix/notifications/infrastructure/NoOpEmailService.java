package com.fairtix.notifications.infrastructure;

import com.fairtix.notifications.application.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(EmailService.class)
public class NoOpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailService.class);

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.debug("Email suppressed (no mail host configured): to={} subject=\"{}\"", to, subject);
    }
}
