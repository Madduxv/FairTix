package com.fairtix.venues.api;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import com.fairtix.venues.application.VenueService;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.dto.CreateVenueRequest;
import com.fairtix.venues.dto.UpdateVenueRequest;
import com.fairtix.venues.dto.VenueResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import java.util.UUID;



@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService service;
    public VenueController(VenueService service){
        this.service = service;
    }

    /**
     * createVenue is responsible for creating new venues as necessary.
     * Accepts a JSON request body that contains the venue's details.
     * @param request the venue as a json payload
     * @return the newly created venue.
     */

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse createVenue(@Valid @RequestBody CreateVenueRequest request){
        Venue venue = service.createVenue(
                request.name(),
                request.address()
        );
        return VenueResponse.from(venue);
    }

    /**
     * Take the details about the venues requested and returns information about that venue.
     * @param name the name of the venue.
     * @param address the address of that venue.
     * @param page the page number.
     * @param size number of items per page.
     * @return the requested page.
     */

    @PermitAll
    @GetMapping
    public Page<VenueResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Venue> venues = service.search(name, address, PageRequest.of(page, Math.min(size, 100)));
        return venues.map(VenueResponse::from);
    }

    @PermitAll
    @GetMapping("/{id}")
    public VenueResponse getVenue(@PathVariable UUID id){
        return VenueResponse.from(service.getVenue(id));
    }

    /**
     * Gets an individual venue based on its id
     * @param id the id of the venue
     * @return the requested venue which matches the id.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public VenueResponse Update(@PathVariable UUID id, @Valid @RequestBody UpdateVenueRequest request){
        Venue updated = service.update(id, request);
        return VenueResponse.from(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id){
        service.delete(id);
    }

}
