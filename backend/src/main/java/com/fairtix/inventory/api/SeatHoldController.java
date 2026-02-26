package com.fairtix.inventory.api;

import com.fairtix.inventory.application.SeatHoldService;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.dto.CreateHoldRequest;
import com.fairtix.inventory.dto.SeatHoldResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
   * GET /api/holds?holderId=...&status=ACTIVE
   *
   * @return 200 OK with the matching holds (empty list if none)
   */
  @GetMapping("/api/holds")
  public List<SeatHoldResponse> listHolds(
      @RequestParam String holderId,
      @RequestParam(defaultValue = "ACTIVE") HoldStatus status) {
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
   * @return 200 OK with the hold, or 404 if not found for this holder
   */
  @PermitAll
  @GetMapping("/api/holds/{holdId}")
  public SeatHoldResponse getHold(
      @PathVariable UUID holdId,
      @RequestParam String holderId) {
    return SeatHoldResponse.from(seatHoldService.getHold(holdId, holderId));
  }

  /**
   * Releases an active hold, freeing the seat for others.
   * Idempotent: calling release on an already-RELEASED hold returns 200.
   *
   * POST /api/holds/{holdId}/release?holderId=...
   *
   * @return 200 OK with the updated hold
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/api/holds/{holdId}/release")
  public SeatHoldResponse releaseHold(
      @PathVariable UUID holdId,
      @RequestParam String holderId) {
    return SeatHoldResponse.from(seatHoldService.releaseHold(holdId, holderId));
  }

  /**
   * Confirms an active hold, transitioning the seat to BOOKED.
   * Idempotent: calling confirm on an already-CONFIRMED hold returns 200.
   *
   * POST /api/holds/{holdId}/confirm?holderId=...
   *
   * @return 200 OK with the updated hold
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/api/holds/{holdId}/confirm")
  public SeatHoldResponse confirmHold(
      @PathVariable UUID holdId,
      @RequestParam String holderId) {
    return SeatHoldResponse.from(seatHoldService.confirmHold(holdId, holderId));
  }
}
