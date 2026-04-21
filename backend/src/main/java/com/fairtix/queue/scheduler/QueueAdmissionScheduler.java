package com.fairtix.queue.scheduler;

import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.notifications.application.EmailService;
import com.fairtix.notifications.application.EmailTemplateService;
import com.fairtix.notifications.application.NotificationPreferenceService;
import com.fairtix.notifications.domain.NotificationPreference;
import com.fairtix.queue.application.QueueService;
import com.fairtix.queue.application.QueueSseService;
import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.users.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class QueueAdmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueAdmissionScheduler.class);

    private final QueueService queueService;
    private final QueueSseService queueSseService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final UserRepository userRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final EventRepository eventRepository;

    public QueueAdmissionScheduler(QueueService queueService,
                                   QueueSseService queueSseService,
                                   EmailService emailService,
                                   EmailTemplateService emailTemplateService,
                                   UserRepository userRepository,
                                   NotificationPreferenceService notificationPreferenceService,
                                   EventRepository eventRepository) {
        this.queueService = queueService;
        this.queueSseService = queueSseService;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.userRepository = userRepository;
        this.notificationPreferenceService = notificationPreferenceService;
        this.eventRepository = eventRepository;
    }

    @Scheduled(fixedDelayString = "${queue.admission.interval-ms:30000}")
    public void admitWaitingUsers() {
        List<UUID> eventIds = queueService.findEventIdsWithWaitingEntries();
        for (UUID eventId : eventIds) {
            try {
                List<QueueEntry> admitted = queueService.admitNextBatch(eventId);
                queueSseService.broadcast(eventId);
                sendAdmissionEmails(admitted, eventId);
            } catch (Exception e) {
                log.error("Failed to admit batch for event {}: {}", eventId, e.getMessage());
            }
        }
    }

    private void sendAdmissionEmails(List<QueueEntry> admitted, UUID eventId) {
        if (admitted.isEmpty()) return;
        String eventTitle = eventRepository.findById(eventId)
                .map(e -> e.getTitle())
                .orElse("the event");
        for (QueueEntry entry : admitted) {
            try {
                NotificationPreference prefs = notificationPreferenceService.getPreferences(entry.getUserId());
                if (!prefs.isEmailTicket()) continue;
                userRepository.findById(entry.getUserId()).ifPresent(user -> {
                    String expiresAt = entry.getExpiresAt() != null ? entry.getExpiresAt().toString() : "soon";
                    String body = emailTemplateService.buildQueueAdmittedEmail(
                            user.getEmail(), eventTitle, expiresAt);
                    emailService.sendEmail(user.getEmail(), "You're admitted — " + eventTitle, body);
                });
            } catch (Exception ex) {
                log.warn("Failed to send queue admission email for user {}: {}", entry.getUserId(), ex.getMessage());
            }
        }
    }
}
