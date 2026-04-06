package com.fairtix.events.dto;

import java.time.Instant;
import java.util.UUID;

import com.fairtix.events.domain.Event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload for an event
 *
 * Returned by event api endpoints
 *
 * @param id        the unique id of the event
 * @param title     the event title
 * @param startTime the event start time in UTC (ISO-8601)
 * @param venue     the name of the venue
 */
@Schema(description = "Event details")
public record EventResponse(
        @Schema(description = "Event ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,
        @Schema(description = "Event title", example = "Summer Music Festival")
        String title,
        @Schema(description = "Start time in UTC", example = "2026-07-15T19:00:00Z")
        Instant startTime,
        @Schema(description = "Venue name", example = "Madison Square Garden")
        String venue,
        @Schema(description = "Organizer user ID")
        UUID organizerId) {

    /**
     * Maps an {@link Event} object to an API response.
     *
     * @param event the event entity
     * @return the corresponding {@link EventResponse}
     */
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getVenue(),
                event.getOrganizerId());
    }
}
