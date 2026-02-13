package com.fairtix.inventory.dto;

import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.SeatHold;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload for a seat hold.
 *
 * @param id        the unique hold id
 * @param seatId    the held seat
 * @param eventId   the event the seat belongs to
 * @param holderId  the holder identifier
 * @param expiresAt when the hold expires (UTC)
 * @param createdAt when the hold was created (UTC)
 * @param status    the current hold status
 */
public record SeatHoldResponse(
    UUID id,
    UUID seatId,
    UUID eventId,
    String holderId,
    Instant expiresAt,
    Instant createdAt,
    HoldStatus status) {

  /**
   * Maps a {@link SeatHold} entity to a {@link SeatHoldResponse}.
   *
   * @param hold the hold entity
   * @return the corresponding response
   */
  public static SeatHoldResponse from(SeatHold hold) {
    return new SeatHoldResponse(
        hold.getId(),
        hold.getSeat().getId(),
        hold.getSeat().getEvent().getId(),
        hold.getHolderId(),
        hold.getExpiresAt(),
        hold.getCreatedAt(),
        hold.getStatus());
  }
}
