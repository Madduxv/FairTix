package com.fairtix.inventory.application;

import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.dto.SeatPositionUpdate;
import com.fairtix.inventory.infrastructure.SeatRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class SeatService {

  private static final Logger log = LoggerFactory.getLogger(SeatService.class);

  private final SeatRepository seatRepository;
  private final EventRepository eventRepository;

  public SeatService(SeatRepository seatRepository, EventRepository eventRepository) {
    this.seatRepository = seatRepository;
    this.eventRepository = eventRepository;
  }

  /**
   * Creates a seat for an event.
   *
   * @param eventId    the event the seat belongs to
   * @param section    the seating section label
   * @param rowLabel   the row label within the section
   * @param seatNumber the individual seat number
   * @return the newly created {@link Seat}
   * @throws IllegalArgumentException if the event is not found
   */
  public Seat createSeat(UUID eventId, String section, String rowLabel, String seatNumber, BigDecimal price) {

    log.info("Creating seat for event {} section={} row={} seat={} price={}",
            eventId, section, rowLabel, seatNumber, price);

    var event = eventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

    if (seatRepository.existsByEvent_IdAndSectionAndRowLabelAndSeatNumber(
            eventId, section, rowLabel, seatNumber)) {
      throw new DuplicateSeatException(
          "Seat already exists: section=%s row=%s seat=%s for event %s"
              .formatted(section, rowLabel, seatNumber, eventId));
    }

    try {
      return seatRepository.save(new Seat(event, section, rowLabel, seatNumber, price));
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateSeatException(
          "Seat already exists: section=%s row=%s seat=%s for event %s"
              .formatted(section, rowLabel, seatNumber, eventId));
    }
  }

  /**
   * Returns all seats for an event.
   *
   * @param eventId the event id
   * @return list of seats
   */
  public List<Seat> getSeatsForEvent(UUID eventId) {

    log.info("Fetching all seats for event {}", eventId);

    var seats = seatRepository.findByEvent_Id(eventId);

    log.info("Found {} seats for event {}", seats.size(), eventId);
    return seats;
  }

  /**
   * Returns only available seats for an event.
   *
   * @param eventId the event id
   * @return list of available seats
   */
  public List<Seat> getAvailableSeatsForEvent(UUID eventId) {

    log.info("Fetching available seats for event {}", eventId);

    var seats = seatRepository.findByEvent_IdAndStatus(eventId, SeatStatus.AVAILABLE);

    log.info("Found {} available seats for event {}", seats.size(), eventId);
    return seats;
  }

  public void bulkUpdatePositions(UUID eventId, List<SeatPositionUpdate> updates, UUID userId) {
    log.info("Bulk updating {} seat positions for event {} by user {}", updates.size(), eventId, userId);
    for (SeatPositionUpdate update : updates) {
      Seat seat = seatRepository.findById(update.id())
          .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + update.id()));
      if (!seat.getEvent().getId().equals(eventId)) {
        throw new IllegalArgumentException("Seat " + update.id() + " does not belong to event " + eventId);
      }
      seat.setPosX(update.posX());
      seat.setPosY(update.posY());
      seat.setRotation(update.rotation());
      seatRepository.save(seat);
    }
  }
}
