package com.fairtix.fairtix.events.dto;

import java.time.Instant;
import java.util.UUID;

import com.fairtix.fairtix.events.domain.Event;

public record EventResponse(
    UUID id,
    String title,
    Instant startTime,
    String venue) {

  public static EventResponse from(Event event) {
    return new EventResponse(
        event.getId(),
        event.getTitle(),
        event.getStartTime(),
        event.getVenue());
  }
}
