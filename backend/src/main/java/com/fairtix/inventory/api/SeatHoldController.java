package com.fairtix.inventory.api;

import com.fairtix.inventory.application.SeatHoldService;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.dto.CreateHoldRequest;
import com.fairtix.inventory.dto.SeatHoldResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Manages temporary seat holds and their lifecycle (create → confirm / release).
 *
 * <p>Holds expire automatically after a configurable duration. Confirm and release
 * operations are idempotent — safe to retry without side effects.
 */
@Tag(name = "Holds", description = "Seat hold lifecycle")
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
     * @param eventId the event whose seats to hold
     * @param request seat IDs, holder identifier, and optional duration
     * @return 201 Created with the list of created holds
     */
    @Operation(summary = "Create a seat hold",
            description = "Authenticated. Temporarily reserves 1–10 seats for the holder.")
    @ApiResponse(responseCode = "201", description = "Holds created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "One or more seats not available")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/events/{eventId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatHoldResponse> createHold(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateHoldRequest request) {
        return seatHoldService
                .createHold(eventId, request.seatIds(), request.holderId(), request.durationMinutes())
                .stream()
                .map(SeatHoldResponse::from)
                .toList();
    }

    /**
     * Lists holds for a given holder, optionally filtered by status.
     *
     * GET /api/holds?holderId=...&amp;status=ACTIVE
     *
     * @param holderId the holder's identifier
     * @param status   hold status filter (defaults to ACTIVE)
     * @return 200 OK with the matching holds (empty list if none)
     */
    @Operation(summary = "List holds for a holder",
            description = "Returns holds matching the given holder and status.")
    @ApiResponse(responseCode = "200", description = "List of holds")
    @GetMapping("/api/holds")
    public List<SeatHoldResponse> listHolds(
            @Parameter(description = "Holder identifier") @RequestParam String holderId,
            @Parameter(description = "Filter by status") @RequestParam(defaultValue = "ACTIVE") HoldStatus status) {
        return seatHoldService.listHolds(holderId, status)
                .stream()
                .map(SeatHoldResponse::from)
                .toList();
    }

    /**
     * Returns hold details visible only to the original holder.
     *
     * GET /api/holds/{holdId}?holderId=...
     *
     * @param holdId   the hold's unique ID
     * @param holderId the holder's identifier (ownership check)
     * @return 200 OK with the hold, or 404 if not found for this holder
     */
    @Operation(summary = "Get a hold by ID",
            description = "Returns a single hold, scoped to the given holder.")
    @ApiResponse(responseCode = "200", description = "Hold found")
    @ApiResponse(responseCode = "404", description = "Hold not found for this holder")
    @PermitAll
    @GetMapping("/api/holds/{holdId}")
    public SeatHoldResponse getHold(
            @PathVariable UUID holdId,
            @Parameter(description = "Holder identifier") @RequestParam String holderId) {
        return SeatHoldResponse.from(seatHoldService.getHold(holdId, holderId));
    }

    /**
     * Releases an active hold, freeing the seat for others.
     * Idempotent: calling release on an already-RELEASED hold returns 200.
     *
     * POST /api/holds/{holdId}/release?holderId=...
     *
     * @param holdId   the hold to release
     * @param holderId the holder's identifier (ownership check)
     * @return 200 OK with the updated hold
     */
    @Operation(summary = "Release a hold",
            description = "Authenticated. Idempotent — frees the seat back to AVAILABLE.")
    @ApiResponse(responseCode = "200", description = "Hold released")
    @ApiResponse(responseCode = "404", description = "Hold not found")
    @ApiResponse(responseCode = "409", description = "Hold already confirmed")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/holds/{holdId}/release")
    public SeatHoldResponse releaseHold(
            @PathVariable UUID holdId,
            @Parameter(description = "Holder identifier") @RequestParam String holderId) {
        return SeatHoldResponse.from(seatHoldService.releaseHold(holdId, holderId));
    }

    /**
     * Confirms an active hold, transitioning the seat to BOOKED.
     * Idempotent: calling confirm on an already-CONFIRMED hold returns 200.
     *
     * POST /api/holds/{holdId}/confirm?holderId=...
     *
     * @param holdId   the hold to confirm
     * @param holderId the holder's identifier (ownership check)
     * @return 200 OK with the updated hold
     */
    @Operation(summary = "Confirm a hold",
            description = "Authenticated. Idempotent — transitions the seat to BOOKED.")
    @ApiResponse(responseCode = "200", description = "Hold confirmed")
    @ApiResponse(responseCode = "404", description = "Hold not found")
    @ApiResponse(responseCode = "409", description = "Hold expired or already released")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/holds/{holdId}/confirm")
    public SeatHoldResponse confirmHold(
            @PathVariable UUID holdId,
            @Parameter(description = "Holder identifier") @RequestParam String holderId) {
        return SeatHoldResponse.from(seatHoldService.confirmHold(holdId, holderId));
    }
}
