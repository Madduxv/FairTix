package com.fairtix.events.api;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fairtix.events.application.EventService;
import com.fairtix.events.domain.Event;
import com.fairtix.events.dto.CreateEventRequest;
import com.fairtix.events.dto.UpdateEventRequest;
import com.fairtix.events.dto.EventResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import java.util.UUID;

/**
 * CRUD operations for events.
 *
 * <p>Read endpoints are public; create, update, and delete require the ADMIN role.
 */
@Tag(name = "Events", description = "Event management")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    /**
     * Creates a new event.
     *
     * Accepts a JSON request body containing event details
     *
     * @param request the requested event as a json payload
     * @return the newly created event
     */
    @Operation(summary = "Create an event", description = "Admin-only. Creates a new event.")
    @ApiResponse(responseCode = "201", description = "Event created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not an admin")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        Event event = service.createEvent(
                request.title(),
                request.startTime(),
                request.venue(),
                request.thumbnail());
        return EventResponse.from(event);
    }

    /**
     * Takes details about the types of events requested and returns a page of that
     * type of event
     *
     * @param venueName the name of the venue
     * @param title     the title of the event
     * @param upcoming  whether or not to only display upcoming events
     *                  (true by default)
     * @param page      the page number
     * @param size      the number of items per page
     * @return the requested page
     */
    @Operation(summary = "Search events",
            description = "Public. Returns a paginated list of events, optionally filtered.")
    @ApiResponse(responseCode = "200", description = "Page of matching events")
    @SecurityRequirements
    @PermitAll
    @GetMapping
    public Page<EventResponse> search(
            @Parameter(description = "Filter by venue name") @RequestParam(required = false) String venueName,
            @Parameter(description = "Filter by title (contains)") @RequestParam(required = false) String title,
            @Parameter(description = "Only future events") @RequestParam(required = false) Boolean upcoming,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {

        Page<Event> events = service.search(venueName, title, upcoming, PageRequest.of(page, Math.min(size, 100)));
        return events.map(EventResponse::from);
    }

    /**
     * Gets a specific event based on its id
     *
     * @param id the id of the event
     * @return the requested event
     */
    @Operation(summary = "Get event by ID", description = "Public. Returns a single event.")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @SecurityRequirements
    @PermitAll
    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable UUID id) {
        return EventResponse.from(service.getEvent(id));
    }

    /**
     * Updates the title or start time of an event
     *
     * @param id      the id of the event
     * @param request an {@link UpdateEventRequest} containing the updated details
     *                of the event
     * @return an {@link EventResponse} containing the newly updated event
     */
    @Operation(summary = "Update an event", description = "Admin-only. Updates title and/or start time.")
    @ApiResponse(responseCode = "200", description = "Event updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public EventResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {
        Event updated = service.update(id, request);
        return EventResponse.from(updated);
    }

    /**
     * Deletes the requested event
     *
     * @param id the UUID id of the event
     */
    @Operation(summary = "Delete an event", description = "Admin-only. Permanently deletes an event.")
    @ApiResponse(responseCode = "204", description = "Event deleted")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
