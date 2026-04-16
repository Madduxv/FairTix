package com.fairtix.queue.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.queue.application.QueueService;
import com.fairtix.queue.domain.QueueEntry;
import com.fairtix.queue.domain.QueueStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Queue", description = "Waiting room and queue token management")
@RestController
@RequestMapping("/api/events/{eventId}/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @Operation(summary = "Join the queue for an event")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinQueueResponse joinQueue(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        QueueEntry entry = queueService.joinQueue(eventId, principal.getUserId());
        long totalAhead = queueService.countWaiting(eventId) - 1;
        return JoinQueueResponse.from(entry, Math.max(0, totalAhead));
    }

    @Operation(summary = "Get queue status for the authenticated user")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/status")
    public QueueStatusResponse getStatus(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        QueueEntry entry = queueService.getStatus(eventId, principal.getUserId());
        long totalAhead = entry.getStatus() == QueueStatus.WAITING
                ? queueService.countWaiting(eventId) - 1
                : 0;
        return QueueStatusResponse.from(entry, Math.max(0, totalAhead));
    }

    @Operation(summary = "Leave the queue voluntarily")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveQueue(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        queueService.leaveQueue(eventId, principal.getUserId());
    }

    @Operation(summary = "Admin: view full queue state for an event")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public List<QueueEntry> adminViewQueue(@PathVariable UUID eventId) {
        return queueService.getQueueForEvent(eventId);
    }

    @Operation(summary = "Admin: manually admit next N users")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/admit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminAdmit(@PathVariable UUID eventId) {
        queueService.admitNextBatch(eventId);
    }
}
