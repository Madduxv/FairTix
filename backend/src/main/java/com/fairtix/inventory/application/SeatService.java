package com.fairtix.inventory.application;

import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SeatService {

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
  public Seat createSeat(UUID eventId, String section, String rowLabel, String seatNumber) {
    var event = eventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    return seatRepository.save(new Seat(event, section, rowLabel, seatNumber));
  }

  /**
   * Returns all seats for an event.
   *
   * @param eventId the event id
   * @return list of seats
   */
  public List<Seat> getSeatsForEvent(UUID eventId) {
    return seatRepository.findByEvent_Id(eventId);
  }

  /**
   * Returns only available seats for an event.
   *
   * @param eventId the event id
   * @return list of available seats
   */
  public List<Seat> getAvailableSeatsForEvent(UUID eventId) {
    return seatRepository.findByEvent_IdAndStatus(eventId, SeatStatus.AVAILABLE);
  }
}
