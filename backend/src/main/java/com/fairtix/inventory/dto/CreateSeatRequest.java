package com.fairtix.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request payload for creating a seat.
 *
 * @param section    the seating section label (e.g. "Floor", "Balcony")
 * @param rowLabel   the row within the section (e.g. "A", "B")
 * @param seatNumber the seat identifier within the row (e.g. "101")
 * @param price      the ticket price for this seat
 */
@Schema(description = "Payload for creating a seat in an event's inventory")
public record CreateSeatRequest(
        @Schema(description = "Seating section label", example = "Floor")
        String section,
        @Schema(description = "Row within the section", example = "A")
        String rowLabel,
        @Schema(description = "Seat identifier within the row", example = "101")
        String seatNumber,
        @NotNull
        @DecimalMin("0.00")
        @Schema(description = "Ticket price for this seat", example = "49.99")
        BigDecimal price) {
}
