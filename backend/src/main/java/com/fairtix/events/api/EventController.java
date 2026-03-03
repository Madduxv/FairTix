package com.fairtix.events.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fairtix.events.application.EventService;
import com.fairtix.events.domain.Event;
import com.fairtix.events.dto.CreateEventRequest;
import com.fairtix.events.dto.UpdateEventRequest;
import com.fairtix.events.dto.EventResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

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
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public EventResponse createEvent(@RequestBody CreateEventRequest request) {
    Event event = service.createEvent(
        request.title(),
        request.startTime(),
        request.venue());
    return EventResponse.from(event);
  }

  /**
   * Takes page number and number of items per page and returns the requested page
   * of events
   *
   * @param page the page number
   * @param size the number of items per page
   * @return the requested page
   */
  @PermitAll
  @GetMapping
  public Page<EventResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<Event> events = service.findAll(PageRequest.of(page, size));
    return (Page<EventResponse>) events.map(EventResponse::from);
  }

  /**
   * Gets a specific event based on its id
   *
   * @param id the id of the event
   * @return the requested event
   */
  @PermitAll
  @GetMapping("/{id}")
  public EventResponse getEvent(@PathVariable UUID id) {
    return EventResponse.from(service.getEvent(id));
  }

  /**
   * Updates the title or start time of an event
   *
   * @param id      the id of the event
   * @param request an {@link UpdateEventRequest} containing the updated details
   *                of the event
   * @return an {@link EventResponse} containing the newly updated event
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public EventResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateEventRequest request) {
    Event updated = service.update(id, request);
    return EventResponse.from(updated);
  }
}
