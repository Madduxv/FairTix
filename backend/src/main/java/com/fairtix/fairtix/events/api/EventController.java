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

  @PostMapping
  public EventResponse createEvent(@RequestBody CreateEventRequest request) {
    Event event = service.createEvent(
        request.title(),
        request.startTime(),
        request.venue());
    return EventResponse.from(event);
  }

  @GetMapping
  public List<EventResponse> getAllEvents() {
    return service.getAllEvents()
        .stream()
        .map(EventResponse::from)
        .toList();
  }

  @GetMapping("/{id}")
  public EventResponse getEvent(@PathVariable UUID id) {
    return EventResponse.from(service.getEvent(id));
  }
}
