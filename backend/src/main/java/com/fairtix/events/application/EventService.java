package com.fairtix.events.application;

import com.fairtix.events.domain.Event;
import com.fairtix.events.dto.UpdateEventRequest;
import com.fairtix.events.infrastructure.EventRepository;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EventService {

  private final EventRepository repository;

  public EventService(EventRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates and persists a new {@link Event}
   *
   * @param title     the title or name of the event
   * @param startTime the {@link Instant} start time of the event in UTC
   * @param venue     the name of the venue for the event
   * @return a newly created event
   */
  public Event createEvent(String title, Instant startTime, String venue) {
    Event event = new Event(title, venue, startTime);
    return repository.save(event);
  }

  /**
   * @param id the id of the event
   * @throws IllegalArgumentException if the event is not found
   * @return the requested {@link Event}
   */
  public Event getEvent(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
  }

  /**
   * Takes in the number of items per page and the page number and returns the
   * requested page of events
   *
   * @param pageable the number of items per page and page number
   * @return the requested page
   */
  public Page<Event> findAll(Pageable pageable) {
    return repository.findAll(pageable);
  }

  /**
   * Updates the title or start time of an event
   *
   * @param id      the id of the event
   * @param request an {@link UpdateEventRequest} containing the title and start
   *                time of the event
   *
   * @throws IllegalArgumentException if the event is not found
   * @return the newly updated event
   */
  public Event update(UUID id, UpdateEventRequest request) {
    Event event = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
    event.update(request.title(), request.startTime());
    return event;
  }
}
