
package com.fairtix.venues.dto;
import java.time.Instant;
import java.util.UUID;

import com.fairtix.venues.domain.Venue;

public record VenueResponse (UUID id, String name, String address) {
    public static VenueResponse from(Venue venue){
        return new VenueResponse(venue.getId(), venue.getName(), venue.getAddress());
    }
}
