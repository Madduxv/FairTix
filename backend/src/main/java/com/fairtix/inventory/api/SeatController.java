package com.fairtix.inventory.api;

import com.fairtix.inventory.application.SeatService;
import com.fairtix.inventory.dto.CreateSeatRequest;
import com.fairtix.inventory.dto.SeatResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the seat inventory for events.
 *
 * <p>Admins can create seats; anyone can list them.
 */
@Tag(name = "Seats", description = "Seat inventory for events")
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
    @Operation(summary = "Create a seat", description = "Admin-only. Adds a seat to an event's inventory.")
    @ApiResponse(responseCode = "201", description = "Seat created")
    @ApiResponse(responseCode = "404", description = "Event not found")
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
    @Operation(summary = "List seats for an event",
            description = "Public. Returns all seats, optionally filtered to available only.")
    @ApiResponse(responseCode = "200", description = "List of seats")
    @SecurityRequirements
    @PermitAll
    @GetMapping
    public List<SeatResponse> getSeats(
            @PathVariable UUID eventId,
            @Parameter(description = "Return only AVAILABLE seats")
            @RequestParam(required = false, defaultValue = "false") boolean availableOnly) {

        log.info("Request to fetch seats for event {} (availableOnly={})",
                eventId, availableOnly);

        var seats = availableOnly
                ? seatService.getAvailableSeatsForEvent(eventId)
                : seatService.getSeatsForEvent(eventId);
        return seats.stream().map(SeatResponse::from).toList();
    }
}
