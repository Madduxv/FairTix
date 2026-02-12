package com.fairtix.events.dto;

import java.time.Instant;
import java.util.UUID;

import com.fairtix.events.domain.Event;

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
public record EventResponse(
    UUID id,
    String title,
    Instant startTime,
    String venue) {

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
        event.getVenue());
  }
}
