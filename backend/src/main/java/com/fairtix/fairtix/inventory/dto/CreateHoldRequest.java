package com.fairtix.fairtix.inventory.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating a seat hold.
 *
 * @param seatIds         the seats to hold
 * @param holderId        opaque identifier for the holder (session/user id)
 * @param durationMinutes how long the hold lasts; uses the server default when null
 */
public record CreateHoldRequest(
        List<UUID> seatIds,
        String holderId,
        Integer durationMinutes) {
}
