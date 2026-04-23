package com.fairtix.inventory.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.inventory.application.SeatHoldService;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.dto.CreateHoldRequest;
import com.fairtix.inventory.dto.SeatHoldResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Manages temporary seat holds and their lifecycle (create → confirm / release).
 *
 * <p>Holds expire automatically after a configurable duration. Confirm and release
 * operations are idempotent — safe to retry without side effects.
 *
 * <p>All hold operations are bound to the authenticated user — the owner is
 * derived from the JWT, never accepted from the client.
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
     * Body: { "seatIds": ["..."], "durationMinutes": 10 }
     *
     * @param eventId   the event whose seats to hold
     * @param request   seat IDs and optional duration
     * @param principal the authenticated user (injected from JWT)
     * @return 201 Created with the list of created holds
     */
    @Operation(summary = "Create a seat hold",
            description = "Authenticated. Temporarily reserves 1–10 seats for the caller.")
    @ApiResponse(responseCode = "201", description = "Holds created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "One or more seats not available")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/events/{eventId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatHoldResponse> createHold(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateHoldRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return seatHoldService
                .createHold(eventId, request.seatIds(), principal.getUserId(), request.durationMinutes())
                .stream()
                .map(SeatHoldResponse::from)
                .toList();
    }

    /**
     * Lists holds for the authenticated user, optionally filtered by status.
     *
     * GET /api/holds?status=ACTIVE
     *
     * @param principal the authenticated user (injected from JWT)
     * @param status    hold status filter (defaults to ACTIVE)
     * @return 200 OK with the matching holds (empty list if none)
     */
    @Operation(summary = "List holds for the authenticated user",
            description = "Returns holds matching the authenticated user and status.")
    @ApiResponse(responseCode = "200", description = "List of holds")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/holds")
    public List<SeatHoldResponse> listHolds(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Filter by status") @RequestParam(defaultValue = "ACTIVE") HoldStatus status) {
        return seatHoldService.listHolds(principal.getUserId(), status)
                .stream()
                .map(SeatHoldResponse::from)
                .toList();
    }

    /**
     * Returns hold details visible only to the original owner.
     *
     * GET /api/holds/{holdId}
     *
     * @param holdId    the hold's unique ID
     * @param principal the authenticated user (injected from JWT)
     * @return 200 OK with the hold, or 404 if not found for this user
     */
    @Operation(summary = "Get a hold by ID",
            description = "Returns a single hold owned by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Hold found")
    @ApiResponse(responseCode = "404", description = "Hold not found for this user")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/holds/{holdId}")
    public SeatHoldResponse getHold(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return SeatHoldResponse.from(seatHoldService.getHold(holdId, principal.getUserId()));
    }

    /**
     * Releases an active hold, freeing the seat for others.
     * Idempotent: calling release on an already-RELEASED hold returns 200.
     *
     * POST /api/holds/{holdId}/release
     *
     * @param holdId    the hold to release
     * @param principal the authenticated user (injected from JWT)
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
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return SeatHoldResponse.from(seatHoldService.releaseHold(holdId, principal.getUserId()));
    }

    /**
     * Confirms an active hold, transitioning the seat to BOOKED.
     * Idempotent: calling confirm on an already-CONFIRMED hold returns 200.
     *
     * POST /api/holds/{holdId}/confirm
     *
     * @param holdId    the hold to confirm
     * @param principal the authenticated user (injected from JWT)
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
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return SeatHoldResponse.from(seatHoldService.confirmHold(holdId, principal.getUserId()));
    }
}
