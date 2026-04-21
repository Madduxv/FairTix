package com.fairtix.queue.scheduler;

import com.fairtix.queue.application.QueueService;
import com.fairtix.queue.application.QueueSseService;
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

    public QueueAdmissionScheduler(QueueService queueService, QueueSseService queueSseService) {
        this.queueService = queueService;
        this.queueSseService = queueSseService;
    }

    @Scheduled(fixedDelayString = "${queue.admission.interval-ms:30000}")
    public void admitWaitingUsers() {
        List<UUID> eventIds = queueService.findEventIdsWithWaitingEntries();
        for (UUID eventId : eventIds) {
            try {
                queueService.admitNextBatch(eventId);
                queueSseService.broadcast(eventId);
            } catch (Exception e) {
                log.error("Failed to admit batch for event {}: {}", eventId, e.getMessage());
            }
        }
    }
}
