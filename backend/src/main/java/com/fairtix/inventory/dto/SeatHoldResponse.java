package com.fairtix.inventory.dto;

import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.SeatHold;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload for a seat hold.
 *
 * @param id        the unique hold id
 * @param seatId    the held seat
 * @param eventId   the event the seat belongs to
 * @param ownerId   the owner's user ID
 * @param expiresAt when the hold expires (UTC)
 * @param createdAt when the hold was created (UTC)
 * @param status    the current hold status
 */
@Schema(description = "Seat hold details")
public record SeatHoldResponse(
        @Schema(description = "Hold ID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
        UUID id,
        @Schema(description = "Held seat ID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID seatId,
        @Schema(description = "Event ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID eventId,
        @Schema(description = "Owner user ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID ownerId,
        @Schema(description = "Hold expiry time in UTC", example = "2026-07-15T19:10:00Z")
        Instant expiresAt,
        @Schema(description = "Hold creation time in UTC", example = "2026-07-15T19:00:00Z")
        Instant createdAt,
        @Schema(description = "Hold status", example = "ACTIVE")
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
                hold.getOwnerId(),
                hold.getExpiresAt(),
                hold.getCreatedAt(),
                hold.getStatus());
    }
}
