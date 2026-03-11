package com.fairtix.venues.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import com.fairtix.venues.application.VenueService;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.dto.CreateVenueRequest;
import com.fairtix.venues.dto.VenueResponse;

import jakarta.annotation.security.PermitAll;

import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
public class VenueController {
    private final VenueService service;

    public VenueController(VenueService service){
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public VenueResponse createVenue(@RequestBody CreateVenueRequest request){
        Venue venue = service.createVenue(request.name(), request.address(), request.address(),
                request.startTime());
        return VenueResponse.from(venue);
    }

    @PermitAll
    @GetMapping
    public Page<VenueResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Venue> venues = service.findAll(PageRequest.of(page, size));
        return (Page<VenueResponse>) venues.map(VenueResponse::from);
    }
    public VenueResponse getVenue(@PathVariable UUID id) {
        return VenueResponse.from(service.getVenue(id));
    }

}
