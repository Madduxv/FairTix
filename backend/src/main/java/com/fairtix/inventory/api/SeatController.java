package com.fairtix.inventory.api;

import com.fairtix.inventory.application.SeatService;
import com.fairtix.inventory.dto.CreateSeatRequest;
import com.fairtix.inventory.dto.SeatResponse;

import jakarta.annotation.security.PermitAll;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/events/{eventId}/seats")
public class SeatController {

  private static final Logger log = LoggerFactory.getLogger(SeatController.class);
  private final SeatService seatService;

  public SeatController(SeatService seatService) {
    this.seatService = seatService;
  }

  /**
   * Creates a single seat for an event.
   * Intended for dev/testing — seed seats before placing holds.
   *
   * @param eventId the owning event
   * @param request the seat details
   * @return the created seat
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SeatResponse createSeat(
      @PathVariable UUID eventId,
      @RequestBody CreateSeatRequest request) {

    log.info("Request to create seat for event {} section={} row={} seat={}",
            eventId, request.section(), request.rowLabel(), request.seatNumber());

    return SeatResponse.from(
        seatService.createSeat(eventId, request.section(), request.rowLabel(), request.seatNumber()));
  }

  /**
   * Lists seats for an event, optionally filtered to available seats only.
   *
   * @param eventId       the event id
   * @param availableOnly when {@code true} only AVAILABLE seats are returned
   * @return list of matching seats
   */
  @PermitAll
  @GetMapping
  public List<SeatResponse> getSeats(
      @PathVariable UUID eventId,
      @RequestParam(required = false, defaultValue = "false") boolean availableOnly) {

    log.info("Request to fetch seats for event {} (availableOnly={})",
            eventId, availableOnly);

    var seats = availableOnly
        ? seatService.getAvailableSeatsForEvent(eventId)
        : seatService.getSeatsForEvent(eventId);
    return seats.stream().map(SeatResponse::from).toList();
  }
}
