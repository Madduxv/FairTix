package com.fairtix.fairtix.inventory.api;

import com.fairtix.fairtix.inventory.application.SeatHoldService;
import com.fairtix.fairtix.inventory.dto.CreateHoldRequest;
import com.fairtix.fairtix.inventory.dto.SeatHoldResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    /**
     * Places a hold on one or more seats for an event.
     *
     * POST /api/events/{eventId}/holds
     * Body: { "seatIds": ["..."], "holderId": "...", "durationMinutes": 10 }
     *
     * @return 201 Created with the list of created holds
     */
    @PostMapping("/api/events/{eventId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatHoldResponse> createHold(
            @PathVariable UUID eventId,
            @RequestBody CreateHoldRequest request) {
        return seatHoldService
                .createHold(eventId, request.seatIds(), request.holderId(), request.durationMinutes())
                .stream()
                .map(SeatHoldResponse::from)
                .toList();
    }

    /**
     * Returns hold details visible only to the original holder.
     *
     * GET /api/holds/{holdId}?holderId=...
     *
     * @return 200 OK with the hold, or 404 if not found for this holder
     */
    @GetMapping("/api/holds/{holdId}")
    public SeatHoldResponse getHold(
            @PathVariable UUID holdId,
            @RequestParam String holderId) {
        return SeatHoldResponse.from(seatHoldService.getHold(holdId, holderId));
    }

    /**
     * Releases an active hold, freeing the seat for others.
     *
     * POST /api/holds/{holdId}/release?holderId=...
     *
     * @return 200 OK with the updated hold
     */
    @PostMapping("/api/holds/{holdId}/release")
    public SeatHoldResponse releaseHold(
            @PathVariable UUID holdId,
            @RequestParam String holderId) {
        return SeatHoldResponse.from(seatHoldService.releaseHold(holdId, holderId));
    }

    /**
     * Confirms an active hold, transitioning the seat to BOOKED.
     *
     * POST /api/holds/{holdId}/confirm?holderId=...
     *
     * @return 200 OK with the updated hold
     */
    @PostMapping("/api/holds/{holdId}/confirm")
    public SeatHoldResponse confirmHold(
            @PathVariable UUID holdId,
            @RequestParam String holderId) {
        return SeatHoldResponse.from(seatHoldService.confirmHold(holdId, holderId));
    }
}
