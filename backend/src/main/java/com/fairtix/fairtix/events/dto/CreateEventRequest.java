package com.fairtix.fairtix.events.dto;

import java.time.Instant;

/**
 * Requests / Creates payload for creating a new event
 * 
 * @param title     the event title
 * @param startTime the event start time in UTC (ISO-8601)
 * @param venue     the venue name
 */
public record CreateEventRequest(
    String title,
    Instant startTime,
    String venue) {
}
