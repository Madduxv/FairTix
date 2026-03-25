package com.fairtix.events.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requests / Creates payload for creating a new event
 * 
 * @param title     the event title
 * @param startTime the event start time in UTC (ISO-8601)
 * @param venue     the venue name
 */
public record CreateEventRequest(
                @NotBlank String title,
                @NotNull Instant startTime,
                @NotBlank String venue) {
}
