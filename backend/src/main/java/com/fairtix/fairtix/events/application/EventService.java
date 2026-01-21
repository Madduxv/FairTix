package com.fairtix.fairtix.events.application;

import com.fairtix.fairtix.events.domain.Event;
import com.fairtix.fairtix.events.infrastructure.EventRepository;

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

  public Event createEvent(String title, Instant startTime, String venue) {
    Event event = new Event(title, venue, startTime);
    return repository.save(event);
  }

  public Event getEvent(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
  }

  public List<Event> getAllEvents() {
    return repository.findAll();
  }

}
