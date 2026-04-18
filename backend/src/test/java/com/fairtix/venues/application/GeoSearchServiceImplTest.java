package com.fairtix.venues.application;

import com.fairtix.events.domain.Event;
import com.fairtix.events.domain.EventStatus;
import com.fairtix.events.dto.NearbyEventResponse;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.infrastructure.VenueDistance;
import com.fairtix.venues.infrastructure.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoSearchServiceImplTest {

    @Mock private VenueRepository venueRepository;
    @Mock private EventRepository eventRepository;
    @InjectMocks private GeoSearchServiceImpl geoSearchService;

    private VenueDistance venueDistance(UUID venueId, double distanceKm) {
        VenueDistance vd = mock(VenueDistance.class);
        when(vd.getVenueId()).thenReturn(venueId);
        when(vd.getDistanceKm()).thenReturn(distanceKm);
        return vd;
    }

    private Event event(UUID venueId, EventStatus status) {
        Venue venue = mock(Venue.class);
        when(venue.getId()).thenReturn(venueId);

        Event e = mock(Event.class);
        when(e.getId()).thenReturn(UUID.randomUUID());
        when(e.getTitle()).thenReturn("Test Event");
        when(e.getStartTime()).thenReturn(Instant.now().plusSeconds(3600));
        when(e.getStatus()).thenReturn(status);
        when(e.getVenue()).thenReturn(venue);
        when(e.getPerformers()).thenReturn(new ArrayList<>());
        return e;
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventsWithinRadiusReturnedSortedByDistance() {
        UUID venueA = UUID.randomUUID();
        UUID venueB = UUID.randomUUID();

        VenueDistance vdA = venueDistance(venueA, 30.0);
        VenueDistance vdB = venueDistance(venueB, 10.0);
        when(venueRepository.findVenuesWithinRadius(0, 0, 50)).thenReturn(List.of(vdA, vdB));

        Event eventA = event(venueA, EventStatus.ACTIVE);
        Event eventB = event(venueB, EventStatus.PUBLISHED);
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(eventA, eventB));

        Page<NearbyEventResponse> result = geoSearchService.findEventsNear(0, 0, 50, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).distanceKm()).isLessThan(result.getContent().get(1).distanceKm());
    }

    @Test
    void returnsEmptyPageWhenNoVenuesWithinRadius() {
        when(venueRepository.findVenuesWithinRadius(0, 0, 50)).thenReturn(List.of());

        Page<NearbyEventResponse> result = geoSearchService.findEventsNear(0, 0, 50, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void paginationLimitsResults() {
        UUID venueId = UUID.randomUUID();
        VenueDistance vd = venueDistance(venueId, 5.0);
        when(venueRepository.findVenuesWithinRadius(0, 0, 50)).thenReturn(List.of(vd));

        Event e1 = event(venueId, EventStatus.ACTIVE);
        Event e2 = event(venueId, EventStatus.ACTIVE);
        Event e3 = event(venueId, EventStatus.ACTIVE);
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(e1, e2, e3));

        Page<NearbyEventResponse> result = geoSearchService.findEventsNear(0, 0, 50, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void secondPageReturnsCorrectOffset() {
        UUID venueId = UUID.randomUUID();
        VenueDistance vd = venueDistance(venueId, 5.0);
        when(venueRepository.findVenuesWithinRadius(0, 0, 50)).thenReturn(List.of(vd));

        Event e1 = event(venueId, EventStatus.ACTIVE);
        Event e2 = event(venueId, EventStatus.ACTIVE);
        Event e3 = event(venueId, EventStatus.ACTIVE);
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(e1, e2, e3));

        Page<NearbyEventResponse> result = geoSearchService.findEventsNear(0, 0, 50, PageRequest.of(1, 2));

        assertThat(result.getContent()).hasSize(1);
    }
}
