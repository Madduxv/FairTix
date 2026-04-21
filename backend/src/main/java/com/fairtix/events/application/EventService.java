package com.fairtix.events.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.events.domain.Event;
import com.fairtix.events.domain.EventStatus;
import com.fairtix.events.dto.UpdateEventRequest;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.refunds.application.RefundService;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.domain.TicketStatus;
import com.fairtix.tickets.infrastructure.TicketRepository;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.infrastructure.VenueRepository;

import jakarta.persistence.criteria.JoinType;
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
  private final VenueRepository venueRepository;
  private final SeatHoldRepository seatHoldRepository;
  private final TicketRepository ticketRepository;
  private final RefundService refundService;

  public EventService(EventRepository repository, VenueRepository venueRepository,
      SeatHoldRepository seatHoldRepository, TicketRepository ticketRepository,
      RefundService refundService) {
    this.repository = repository;
    this.venueRepository = venueRepository;
    this.seatHoldRepository = seatHoldRepository;
    this.ticketRepository = ticketRepository;
    this.refundService = refundService;
  }

  public Event createEvent(String title, Instant startTime, UUID venueId, UUID organizerId,
      String thumbnail, boolean queueRequired, Integer queueCapacity, Integer maxTicketsPerUser) {
    Venue venue = venueId != null
        ? venueRepository.findById(venueId)
            .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId))
        : null;
    Event event = new Event(title, venue, startTime, thumbnail, organizerId);
    event.updateQueueSettings(queueRequired, queueCapacity);
    event.setMaxTicketsPerUser(maxTicketsPerUser);
    return repository.save(event);
  }

  public Event createEvent(String title, Instant startTime, UUID venueId, UUID organizerId,
      boolean queueRequired, Integer queueCapacity, Integer maxTicketsPerUser) {
    return createEvent(title, startTime, venueId, organizerId, null, queueRequired, queueCapacity, maxTicketsPerUser);
  }

  public Event createEvent(String title, Instant startTime, String venueName, String thumbnail) {
    Venue venue = null;
    if (venueName != null && !venueName.isBlank()) {
      venue = venueRepository.findByName(venueName.trim())
          .orElseGet(() -> venueRepository.save(new Venue(venueName.trim(), null, null, null, null)));
    }
    Event event = new Event(title, venue, startTime, thumbnail, null);
    return repository.save(event);
  }

  public Event getEvent(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
  }

  public Event update(UUID id, UpdateEventRequest request, UUID callerId) {
    Event event = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    verifyOwnership(event, callerId);
    event.update(request.title(), request.startTime(), request.thumbnail());
    if (request.queueRequired() != null || request.queueCapacity() != null) {
      boolean qr = request.queueRequired() != null ? request.queueRequired() : event.isQueueRequired();
      event.updateQueueSettings(qr, request.queueCapacity());
    }
    if (request.maxTicketsPerUser() != null) {
      event.setMaxTicketsPerUser(request.maxTicketsPerUser());
    }
    return event;
  }

  public void delete(UUID id, UUID callerId) {
    Event event = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    verifyOwnership(event, callerId);
    repository.delete(event);
  }

  // --- Lifecycle transitions ---

  public Event publishEvent(UUID eventId, UUID callerId) {
    Event event = repository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    verifyOwnership(event, callerId);
    event.publish();
    return event;
  }

  public Event activateEvent(UUID eventId, UUID callerId) {
    Event event = repository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    verifyOwnership(event, callerId);
    event.activate();
    return event;
  }

  public Event completeEvent(UUID eventId, UUID callerId) {
    Event event = repository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    verifyOwnership(event, callerId);
    event.complete();
    return event;
  }

  public Event cancelEvent(UUID eventId, UUID callerId, String reason) {
    Event event = repository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    verifyOwnership(event, callerId);
    event.cancel(reason);

    // Release all ACTIVE holds for this event
    List<SeatHold> activeHolds = seatHoldRepository.findAllBySeat_Event_IdAndStatus(eventId, HoldStatus.ACTIVE);
    for (SeatHold hold : activeHolds) {
      hold.setStatus(HoldStatus.RELEASED);
      hold.getSeat().setStatus(SeatStatus.AVAILABLE);
    }
    seatHoldRepository.saveAll(activeHolds);

    // Cancel all VALID tickets and auto-process refunds for completed orders
    refundService.processCancellationRefunds(eventId, callerId);

    // Mark any remaining VALID tickets (those without completed orders) as CANCELLED
    List<Ticket> validTickets = ticketRepository.findAllByEvent_IdAndStatus(eventId, TicketStatus.VALID);
    for (Ticket ticket : validTickets) {
      ticket.setStatus(TicketStatus.CANCELLED);
    }
    ticketRepository.saveAll(validTickets);

    return event;
  }

  public Event archiveEvent(UUID eventId, UUID callerId) {
    Event event = repository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    verifyOwnership(event, callerId);
    event.archive();
    return event;
  }

  // --- Search ---

  public Page<Event> search(
      String venue,
      String title,
      Boolean upcoming,
      EventStatus status,
      boolean adminView,
      Pageable pageable) {

    Specification<Event> spec = (root, query, cb) -> {

      List<Predicate> predicates = new ArrayList<>();

      if (venue != null && !venue.isBlank()) {
        predicates.add(
            cb.like(
                cb.lower(root.join("venue", JoinType.LEFT).get("name")),
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

      if (status != null) {
        // Explicit status filter requested
        predicates.add(cb.equal(root.get("status"), status));
      } else if (!adminView) {
        // Public view: only show PUBLISHED and ACTIVE events
        predicates.add(root.get("status").in(EventStatus.PUBLISHED, EventStatus.ACTIVE));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };

    return repository.findAll(spec, pageable);
  }

  private void verifyOwnership(Event event, UUID callerId) {
    if (event.getOrganizerId() != null && !event.getOrganizerId().equals(callerId)) {
      throw new org.springframework.security.access.AccessDeniedException(
          "You do not own this event");
    }
  }
}
