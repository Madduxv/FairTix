package com.fairtix.events.api;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fairtix.audit.application.AuditService;
import com.fairtix.auth.domain.CustomUserPrincipal;
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
 * Update and delete also enforce organizer ownership.
 */
@Tag(name = "Events", description = "Event management")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final AuditService auditService;

    public EventController(EventService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @Operation(summary = "Create an event", description = "Admin-only. Creates a new event owned by the caller.")
    @ApiResponse(responseCode = "201", description = "Event created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not an admin")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request) {
        Event event = service.createEvent(
                request.title(),
                request.startTime(),
                request.venue(),
                principal.getUserId());
        auditService.log(principal.getUserId(), "CREATE", "EVENT", event.getId(),
                "Created event: " + event.getTitle());
        return EventResponse.from(event);
    }

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

    @Operation(summary = "Get event by ID", description = "Public. Returns a single event.")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @SecurityRequirements
    @PermitAll
    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable UUID id) {
        return EventResponse.from(service.getEvent(id));
    }

    @Operation(summary = "Update an event", description = "Admin-only. Only the organizer can update.")
    @ApiResponse(responseCode = "200", description = "Event updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not the organizer")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public EventResponse update(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {
        Event updated = service.update(id, request, principal.getUserId());
        auditService.log(principal.getUserId(), "UPDATE", "EVENT", id,
                "Updated event: " + updated.getTitle());
        return EventResponse.from(updated);
    }

    @Operation(summary = "Delete an event", description = "Admin-only. Only the organizer can delete.")
    @ApiResponse(responseCode = "204", description = "Event deleted")
    @ApiResponse(responseCode = "403", description = "Not the organizer")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        service.delete(id, principal.getUserId());
        auditService.log(principal.getUserId(), "DELETE", "EVENT", id, "Deleted event");
    }
}
