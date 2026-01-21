package com.fairtix.fairtix.events.dto;

import java.time.Instant;

public record CreateEventRequest(
    String title,
    Instant startTime,
    String venue) {
}
