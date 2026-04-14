package com.fairtix.events.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.events.domain.Event;
import com.fairtix.events.dto.UpdateEventRequest;
import com.fairtix.events.infrastructure.EventRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
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
    return repository
        .findByTitleAndStartTimeAndVenue(title, startTime, venue)
        .orElseGet(() -> {
          Event event = new Event(title, venue, startTime);
          return repository.save(event);
        });
  }

  /**
   * Creates and persists a new {@link Event}
   *
   * @param title     the title or name of the event
   * @param startTime the {@link Instant} start time of the event in UTC
   * @param venue     the name of the venue for the event
   * @param thumbnail the url for the thumbnail
   * @return a newly created event
   */
  public Event createEvent(String title, Instant startTime, String venue, String thumbnail) {
    return repository
        .findByTitleAndStartTimeAndVenue(title, startTime, venue)
        .orElseGet(() -> {
          Event event = new Event(title, venue, startTime, thumbnail);
          return repository.save(event);
        });
  }

  /**
   * @param id the id of the event
   * @throws ResourceNotFoundException if the event is not found
   * @return the requested {@link Event}
   */
  public Event getEvent(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
  }

  /**
   * Updates the title or start time of an event
   *
   * @param id      the id of the event
   * @param request an {@link UpdateEventRequest} containing the title and start
   *                time of the event
   *
   * @throws ResourceNotFoundException if the event is not found
   * @return the newly updated event
   */
  public Event update(UUID id, UpdateEventRequest request) {
    Event event = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    event.update(request.title(), request.startTime(), request.thumbnail());
    return event;
  }

  /**
   * Deletes the requested event
   *
   * @param id the id of the event
   * @throws ResourceNotFoundException if an event with the requested id does not
   *                                   exist
   */
  public void delete(UUID id) {
    Event event = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    repository.delete(event);
  }

  /**
   * @param venue
   * @param title
   * @param upcoming
   * @param pageable
   * @return
   */
  public Page<Event> search(
      String venue,
      String title,
      Boolean upcoming,
      Pageable pageable) {

    Specification<Event> spec = (root, query, cb) -> {

      List<Predicate> predicates = new ArrayList<>();

      if (venue != null && !venue.isBlank()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("venue")),
                "%" + venue.toLowerCase() + "%"));
      }

      if (title != null && !title.isBlank()) {
        predicates.add(
            cb.like(
                cb.lower(root.get("title")),
                "%" + title.toLowerCase() + "%"));
      }

      if (upcoming == null || upcoming) {
        predicates.add(
            cb.greaterThan(
                root.get("startTime"),
                Instant.now()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };

    return repository.findAll(spec, pageable);
  }
}
