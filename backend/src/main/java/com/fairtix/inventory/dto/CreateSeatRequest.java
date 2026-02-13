package com.fairtix.inventory.dto;

/**
 * Request payload for creating a seat.
 *
 * @param section    the seating section label (e.g. "Floor", "Balcony")
 * @param rowLabel   the row within the section (e.g. "A", "B")
 * @param seatNumber the seat identifier within the row (e.g. "101")
 */
public record CreateSeatRequest(
    String section,
    String rowLabel,
    String seatNumber) {
}
