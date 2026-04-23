package com.fairtix.venues.application;

import com.fairtix.events.dto.NearbyEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GeoSearchService {
    Page<NearbyEventResponse> findEventsNear(double lat, double lon, double radiusKm, Pageable pageable);
}
