package com.fairtix.venues.application;

import com.fairtix.events.domain.Event;
import com.fairtix.events.domain.EventStatus;
import com.fairtix.events.dto.NearbyEventResponse;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.venues.infrastructure.VenueDistance;
import com.fairtix.venues.infrastructure.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GeoSearchServiceImpl implements GeoSearchService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    public GeoSearchServiceImpl(VenueRepository venueRepository, EventRepository eventRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public Page<NearbyEventResponse> findEventsNear(double lat, double lon, double radiusKm, Pageable pageable) {
        List<VenueDistance> nearbyVenues = venueRepository.findVenuesWithinRadius(lat, lon, radiusKm);
        if (nearbyVenues.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<UUID, Double> distanceByVenueId = nearbyVenues.stream()
                .collect(Collectors.toMap(VenueDistance::getVenueId, VenueDistance::getDistanceKm));

        List<UUID> venueIds = nearbyVenues.stream()
                .map(VenueDistance::getVenueId)
                .collect(Collectors.toList());

        Specification<Event> spec = (root, query, cb) -> cb.and(
                root.get("venue").get("id").in(venueIds),
                root.get("status").in(EventStatus.PUBLISHED, EventStatus.ACTIVE)
        );

        List<Event> allEvents = eventRepository.findAll(spec);

        List<NearbyEventResponse> sorted = allEvents.stream()
                .map(e -> NearbyEventResponse.from(e,
                        distanceByVenueId.getOrDefault(e.getVenue().getId(), Double.MAX_VALUE)))
                .sorted(java.util.Comparator.comparingDouble(NearbyEventResponse::distanceKm))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<NearbyEventResponse> page = start >= sorted.size() ? List.of() : sorted.subList(start, end);

        return new PageImpl<>(page, pageable, sorted.size());
    }
}
