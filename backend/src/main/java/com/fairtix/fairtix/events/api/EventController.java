package com.fairtix.fairtix.events.api;

import org.springframework.web.bind.annotation.*;

import com.fairtix.fairtix.events.application.EventService;
import com.fairtix.fairtix.events.domain.Event;
import com.fairtix.fairtix.events.dto.CreateEventRequest;
import com.fairtix.fairtix.events.dto.EventResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService service;

  public EventController(EventService service) {
    this.service = service;
  }

  /**
   * Creates a new event.
   *
   * Accepts a JSON request body containing event details
   * 
   * @param request the requested event as a json payload
   * @return the newly created event
   */
  @PostMapping
  public EventResponse createEvent(@RequestBody CreateEventRequest request) {
    Event event = service.createEvent(
        request.title(),
        request.startTime(),
        request.venue());
    return EventResponse.from(event);
  }

  /**
   * Gets all events
   *
   * @return a list of all events
   */
  @GetMapping
  public List<EventResponse> getAllEvents() {
    return service.getAllEvents()
        .stream()
        .map(EventResponse::from)
        .toList();
  }

  /**
   * Gets a specific event based on its id
   *
   * @param id the id of the event
   * @return the requested event
   */
  @GetMapping("/{id}")
  public EventResponse getEvent(@PathVariable UUID id) {
    return EventResponse.from(service.getEvent(id));
  }
}
