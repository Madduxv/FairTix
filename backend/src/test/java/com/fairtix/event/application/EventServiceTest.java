package com.fairtix.event.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.events.application.EventService;
import com.fairtix.events.domain.Event;
import com.fairtix.events.dto.UpdateEventRequest;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.infrastructure.VenueRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class EventServiceTest {

  @Autowired
  private EventService eventService;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private VenueRepository venueRepository;

  private Event testEvent;
  private Venue testVenue;

  @BeforeEach
  void setUp() {
    testVenue = venueRepository.save(new Venue("Test Venue", null, null, null, null));
    testEvent = eventRepository.save(
        new Event(
            "Test Event",
            testVenue,
            Instant.now().plusSeconds(3600),
            null));
  }

  // -------------------------------------------------------------------------
  // Create Event
  // -------------------------------------------------------------------------

  @Test
  void creatingEventSucceeds() {
    Venue venue = venueRepository.save(new Venue("New Venue", null, null, null, null));

    Event event = eventService.createEvent(
        "New Event",
        Instant.now().plusSeconds(7200),
        venue.getId(),
        null, false, null, null);

    assertThat(event.getId()).isNotNull();
    assertThat(event.getTitle()).isEqualTo("New Event");
    assertThat(event.getVenue().getName()).isEqualTo("New Venue");
  }

  // -------------------------------------------------------------------------
  // Get Event
  // -------------------------------------------------------------------------

  @Test
  void gettingExistingEventReturnsEvent() {

    Event event = eventService.getEvent(testEvent.getId());

    assertThat(event.getId()).isEqualTo(testEvent.getId());
    assertThat(event.getTitle()).isEqualTo("Test Event");
  }

  @Test
  void gettingNonexistentEventThrowsException() {

    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> eventService.getEvent(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event not found");
  }

  // -------------------------------------------------------------------------
  // Update Event
  // -------------------------------------------------------------------------

  @Test
  void updatingEventChangesTitleAndStartTime() {

    Instant newStart = Instant.now().plusSeconds(7200);

    UpdateEventRequest request = new UpdateEventRequest("Updated Event", newStart, null, null, null);

    Event updated = eventService.update(testEvent.getId(), request, null);

    assertThat(updated.getTitle()).isEqualTo("Updated Event");
    assertThat(updated.getStartTime()).isEqualTo(newStart);
  }

  @Test
  void updatingNonexistentEventThrowsException() {

    UpdateEventRequest request = new UpdateEventRequest("Updated", Instant.now(), null, null, null);

    assertThatThrownBy(() -> eventService.update(UUID.randomUUID(), request, null))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event not found");
  }

  // -------------------------------------------------------------------------
  // Delete Event
  // -------------------------------------------------------------------------

  @Test
  void deletingExistingEventRemovesIt() {

    UUID id = testEvent.getId();

    eventService.delete(id, null);

    assertThat(eventRepository.findById(id)).isEmpty();
  }

  @Test
  void deletingNonexistentEventThrowsException() {

    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> eventService.delete(id, null))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event not found");
  }

  // -------------------------------------------------------------------------
  // Search
  // -------------------------------------------------------------------------

  @Test
  void searchingByVenueReturnsMatchingEvents() {

    Venue anotherVenue = venueRepository.save(new Venue("Another Venue", null, null, null, null));
    eventRepository.save(
        new Event("Another Event", anotherVenue,
            Instant.now().plusSeconds(3600), null));

    Page<Event> results = eventService.search(
        "Test Venue",
        null,
        true,
        PageRequest.of(0, 10));

    assertThat(results.getContent())
        .extracting(e -> e.getVenue().getName())
        .allMatch(v -> v.toLowerCase().contains("test venue"));
  }

  @Test
  void searchingByTitleReturnsMatchingEvents() {

    Page<Event> results = eventService.search(
        null,
        "Test",
        true,
        PageRequest.of(0, 10));

    assertThat(results.getContent())
        .extracting(Event::getTitle)
        .allMatch(t -> t.toLowerCase().contains("test"));
  }

  @Test
  void searchingUpcomingFiltersPastEvents() {

    eventRepository.save(
        new Event("Past Event", testVenue,
            Instant.now().minusSeconds(3600), null));

    Page<Event> results = eventService.search(
        null,
        null,
        true,
        PageRequest.of(0, 10));

    assertThat(results.getContent())
        .allMatch(e -> e.getStartTime().isAfter(Instant.now()));
  }

  @Test
  void searchingWithUpcomingFalseReturnsPastEventsToo() {

    eventRepository.save(
        new Event("Past Event", testVenue,
            Instant.now().minusSeconds(3600), null));

    Page<Event> results = eventService.search(
        null,
        null,
        false,
        PageRequest.of(0, 10));

    assertThat(results.getContent()).isNotEmpty();
  }
}
