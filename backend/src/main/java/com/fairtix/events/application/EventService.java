package com.fairtix.events.application;

import com.fairtix.events.domain.Event;
import com.fairtix.events.infrastructure.EventRepository;

import jakarta.transaction.Transactional;

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
   * @return a list containing all events
   */
  public List<Event> getAllEvents() {
    return repository.findAll();
  }

}
