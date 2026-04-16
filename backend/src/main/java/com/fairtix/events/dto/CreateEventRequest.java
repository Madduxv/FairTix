package com.fairtix.events.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requests / Creates payload for creating a new event
 *
 * @param title     the event title
 * @param startTime the event start time in UTC (ISO-8601)
 * @param venue     the venue name
 */
@Schema(description = "Payload for creating a new event")
public record CreateEventRequest(
        @Schema(description = "Event title", example = "Summer Music Festival")
        @NotBlank String title,
        @Schema(description = "Event start time in UTC (ISO-8601)", example = "2026-07-15T19:00:00Z")
        @NotNull Instant startTime,
        @Schema(description = "Venue name", example = "Madison Square Garden")
        @NotBlank String venue,
        @Schema(description = "Whether this event requires queue admission before seat holds", example = "false")
        Boolean queueRequired,
        @Schema(description = "Maximum queue capacity (null = unlimited)")
        Integer queueCapacity) {
}
