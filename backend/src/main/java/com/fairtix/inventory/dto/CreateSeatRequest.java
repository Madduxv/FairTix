package com.fairtix.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for creating a seat.
 *
 * @param section    the seating section label (e.g. "Floor", "Balcony")
 * @param rowLabel   the row within the section (e.g. "A", "B")
 * @param seatNumber the seat identifier within the row (e.g. "101")
 */
@Schema(description = "Payload for creating a seat in an event's inventory")
public record CreateSeatRequest(
        @Schema(description = "Seating section label", example = "Floor")
        String section,
        @Schema(description = "Row within the section", example = "A")
        String rowLabel,
        @Schema(description = "Seat identifier within the row", example = "101")
        String seatNumber) {
}
