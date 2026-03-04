package com.fairtix.events.api;

import org.springframework.http.HttpStatus;
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
   * Takes details about the types of events requested and returns a page of that
   * type of event
   *
   * @param venue    the name of the venue
   * @param title    the title of the event
   * @param upcoming whether or not to only display upcoming events
   *                 (true by default)
   * @param page     the page number
   * @param size     the number of items per page
   * @return the requested page
   */
  @PermitAll
  @GetMapping
  public Page<EventResponse> list(
      @RequestParam(required = false) String venue,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) Boolean upcoming,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Page<Event> events = service.search(venue, title, upcoming, PageRequest.of(page, size));
    return events.map(EventResponse::from);
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

  /**
   * Deletes the requested event
   *
   * @param id the UUID id of the event
   */
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
