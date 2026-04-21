package com.fairtix.queue.application;

import com.fairtix.queue.api.QueueStatusResponse;
import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class QueueSseService {

    private static final Logger log = LoggerFactory.getLogger(QueueSseService.class);
    private static final long SSE_TIMEOUT_MS = 180_000L;

    private record EmitterEntry(UUID userId, SseEmitter emitter) {}

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<EmitterEntry>> registry = new ConcurrentHashMap<>();
    private final QueueService queueService;

    public QueueSseService(QueueService queueService) {
        this.queueService = queueService;
    }

    public SseEmitter register(UUID eventId, UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        EmitterEntry entry = new EmitterEntry(userId, emitter);
        registry.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(entry);

        Runnable remove = () -> removeEntry(eventId, entry);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            QueueEntry queueEntry = queueService.getStatus(eventId, userId);
            long totalAhead = queueEntry.getStatus() == QueueStatus.WAITING
                    ? Math.max(0, queueEntry.getPosition() - 1)
                    : 0;
            emitter.send(SseEmitter.event()
                    .data(QueueStatusResponse.from(queueEntry, totalAhead), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("Initial SSE send failed for event {} user {}: {}", eventId, userId, e.getMessage());
            emitter.completeWithError(e);
            removeEntry(eventId, entry);
        }

        return emitter;
    }

    public void broadcast(UUID eventId) {
        CopyOnWriteArrayList<EmitterEntry> entries = registry.get(eventId);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (EmitterEntry entry : entries) {
            try {
                QueueEntry queueEntry = queueService.getStatus(eventId, entry.userId());
                long totalAhead = queueEntry.getStatus() == QueueStatus.WAITING
                        ? Math.max(0, queueEntry.getPosition() - 1)
                        : 0;
                entry.emitter().send(SseEmitter.event()
                        .data(QueueStatusResponse.from(queueEntry, totalAhead), MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.warn("SSE broadcast failed for event {} user {}: {}", eventId, entry.userId(), e.getMessage());
                entry.emitter().completeWithError(e);
                removeEntry(eventId, entry);
            }
        }
    }

    private void removeEntry(UUID eventId, EmitterEntry entry) {
        CopyOnWriteArrayList<EmitterEntry> entries = registry.get(eventId);
        if (entries != null) {
            entries.remove(entry);
        }
    }
}
