package com.fairtix.inventory.dto;

import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Seat details")
public record SeatResponse(
        @Schema(description = "Seat ID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID id,
        @Schema(description = "Event ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID eventId,
        @Schema(description = "Seating section", example = "Floor")
        String section,
        @Schema(description = "Row label", example = "A")
        String rowLabel,
        @Schema(description = "Seat number", example = "101")
        String seatNumber,
        @Schema(description = "Availability status", example = "AVAILABLE")
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
