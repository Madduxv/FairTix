package com.fairtix.queue.scheduler;

import com.fairtix.queue.application.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class QueueExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueExpirationScheduler.class);

    private final QueueService queueService;

    public QueueExpirationScheduler(QueueService queueService) {
        this.queueService = queueService;
    }

    @Scheduled(fixedDelayString = "${queue.expiration.interval-ms:30000}")
    public void expireAdmissions() {
        List<UUID> eventIds = queueService.findEventIdsWithExpiredAdmissions();
        for (UUID eventId : eventIds) {
            try {
                queueService.expireAdmissions(eventId);
            } catch (Exception e) {
                log.error("Failed to expire admissions for event {}: {}", eventId, e.getMessage());
            }
        }
    }
}
