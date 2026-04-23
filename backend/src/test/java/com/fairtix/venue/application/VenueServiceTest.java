package com.fairtix.venue.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.venues.application.VenueService;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.dto.CreateVenueRequest;
import com.fairtix.venues.dto.UpdateVenueRequest;
import com.fairtix.venues.infrastructure.VenueRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class VenueServiceTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Autowired
    private VenueService venueService;

    @Autowired
    private VenueRepository venueRepository;

    private Venue testVenue;

    @BeforeEach
    void setUp() {
        testVenue = venueRepository.save(new Venue("Test Venue", "Test Address", null, null, null, null, null));
    }

    // -------------------------------------------------------------------------
    // Create Venue
    // -------------------------------------------------------------------------

    @Test
    void creatingVenueSucceeds() {
        Venue venue = venueService.createVenue(
                new CreateVenueRequest("New Venue", "New Address", null, null, null, null, null),
                ACTOR_ID);

        assertThat(venue.getId()).isNotNull();
        assertThat(venue.getName()).isEqualTo("New Venue");
        assertThat(venue.getAddress()).isEqualTo("New Address");
    }

    // -------------------------------------------------------------------------
    // Get Venue
    // -------------------------------------------------------------------------

    @Test
    void gettingExistingVenueReturnsVenue() {
        Venue venue = venueService.getVenue(testVenue.getId());

        assertThat(venue.getId()).isEqualTo(testVenue.getId());
        assertThat(venue.getAddress()).isEqualTo("Test Address");
    }

    @Test
    void gettingNonexistentVenueThrowsException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> venueService.getVenue(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found");
    }

    // -------------------------------------------------------------------------
    // Update Venue
    // -------------------------------------------------------------------------

    @Test
    void updatingVenueChangesNameAndAddress() {
        UpdateVenueRequest request = new UpdateVenueRequest("Updated Venue", "Updated Address", null, null, null, null, null);

        Venue updated = venueService.updateVenue(testVenue.getId(), request, ACTOR_ID);

        assertThat(updated.getName()).isEqualTo("Updated Venue");
        assertThat(updated.getAddress()).isEqualTo("Updated Address");
    }

    @Test
    void updatingNonexistentVenueThrowsException() {
        UpdateVenueRequest request = new UpdateVenueRequest("Updated", "Updated", null, null, null, null, null);

        assertThatThrownBy(() -> venueService.updateVenue(UUID.randomUUID(), request, ACTOR_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found");
    }

    // -------------------------------------------------------------------------
    // Delete Venue
    // -------------------------------------------------------------------------

    @Test
    void deletingExistingVenueRemovesIt() {
        UUID id = testVenue.getId();

        venueService.deleteVenue(id, ACTOR_ID);

        assertThat(venueRepository.findById(id)).isEmpty();
    }

    @Test
    void deletingNonexistentVenueThrowsException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> venueService.deleteVenue(id, ACTOR_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found");
    }
}
