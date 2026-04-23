package com.fairtix.venues.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.venues.application.VenueService;
import com.fairtix.venues.dto.CreateVenueRequest;
import com.fairtix.venues.dto.UpdateVenueRequest;
import com.fairtix.venues.dto.VenueResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Venues", description = "Venue management")
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService service;

    public VenueController(VenueService service) {
        this.service = service;
    }

    @Operation(summary = "Create a venue", description = "Admin-only.")
    @ApiResponse(responseCode = "201", description = "Venue created")
    @ApiResponse(responseCode = "409", description = "Name already exists")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse create(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateVenueRequest request) {
        return VenueResponse.from(service.createVenue(request, principal.getUserId()));
    }

    @Operation(summary = "List venues", description = "Public. Returns a paginated list of venues.")
    @ApiResponse(responseCode = "200", description = "Page of venues")
    @PermitAll
    @GetMapping
    public Page<VenueResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.listVenues(PageRequest.of(page, Math.min(size, 100)))
                .map(VenueResponse::from);
    }

    @Operation(summary = "Get venue by ID", description = "Public.")
    @ApiResponse(responseCode = "200", description = "Venue found")
    @ApiResponse(responseCode = "404", description = "Venue not found")
    @PermitAll
    @GetMapping("/{id}")
    public VenueResponse get(@PathVariable UUID id) {
        return VenueResponse.from(service.getVenue(id));
    }

    @Operation(summary = "Update a venue", description = "Admin-only.")
    @ApiResponse(responseCode = "200", description = "Venue updated")
    @ApiResponse(responseCode = "404", description = "Venue not found")
    @ApiResponse(responseCode = "409", description = "Name already exists")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public VenueResponse update(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVenueRequest request) {
        return VenueResponse.from(service.updateVenue(id, request, principal.getUserId()));
    }

    @Operation(summary = "Delete a venue", description = "Admin-only. Fails if events reference this venue.")
    @ApiResponse(responseCode = "204", description = "Venue deleted")
    @ApiResponse(responseCode = "404", description = "Venue not found")
    @ApiResponse(responseCode = "409", description = "Venue has associated events")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        service.deleteVenue(id, principal.getUserId());
    }
}
