package com.fairtix.inventory.dto;

import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;

import java.util.UUID;

/**
 * Response payload for a seat.
 *
 * @param id         the unique seat id
 * @param eventId    the event this seat belongs to
 * @param section    the seating section
 * @param rowLabel   the row within the section
 * @param seatNumber the individual seat number
 * @param status     the current availability status
 */
public record SeatResponse(
    UUID id,
    UUID eventId,
    String section,
    String rowLabel,
    String seatNumber,
    SeatStatus status) {

  /**
   * Maps a {@link Seat} entity to a {@link SeatResponse}.
   *
   * @param seat the seat entity
   * @return the corresponding response
   */
  public static SeatResponse from(Seat seat) {
    return new SeatResponse(
        seat.getId(),
        seat.getEvent().getId(),
        seat.getSection(),
        seat.getRowLabel(),
        seat.getSeatNumber(),
        seat.getStatus());
  }
}
