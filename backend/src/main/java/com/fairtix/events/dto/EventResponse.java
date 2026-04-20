package com.fairtix.events.dto;

import java.time.Instant;
import java.util.UUID;

import com.fairtix.events.domain.Event;
import com.fairtix.events.domain.EventStatus;
import com.fairtix.venues.dto.VenueResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload for an event
 *
 * Returned by event api endpoints
 *
 * @param id        the unique id of the event
 * @param title     the event title
 * @param startTime the event start time in UTC (ISO-8601)
 * @param venue     the venue details
 */
@Schema(description = "Event details")
public record EventResponse(
        @Schema(description = "Event ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,
        @Schema(description = "Event title", example = "Summer Music Festival")
        String title,
        @Schema(description = "Start time in UTC", example = "2026-07-15T19:00:00Z")
        Instant startTime,
        @Schema(description = "Venue details")
        VenueResponse venue,
        @Schema(description = "Organizer user ID")
        UUID organizerId,
        @Schema(description = "Whether this event requires queue admission before seat holds")
        boolean queueRequired,
        @Schema(description = "Maximum queue capacity (null = unlimited)")
        Integer queueCapacity,
        @Schema(description = "Maximum tickets a single user may purchase for this event (null = no cap)")
        Integer maxTicketsPerUser,
        @Schema(description = "Lifecycle status of the event")
        EventStatus status,
        @Schema(description = "When the event was published (null if not yet published)")
        Instant publishedAt,
        @Schema(description = "When the event was cancelled (null if not cancelled)")
        Instant cancelledAt,
        @Schema(description = "When the event was completed (null if not completed)")
        Instant completedAt,
        @Schema(description = "When the event was archived (null if not archived)")
        Instant archivedAt,
        @Schema(description = "Reason for cancellation (null if not cancelled)")
        String cancellationReason) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getVenue() != null ? VenueResponse.from(event.getVenue()) : null,
                event.getOrganizerId(),
                event.isQueueRequired(),
                event.getQueueCapacity(),
                event.getMaxTicketsPerUser(),
                event.getStatus(),
                event.getPublishedAt(),
                event.getCancelledAt(),
                event.getCompletedAt(),
                event.getArchivedAt(),
                event.getCancellationReason());
    }
}
