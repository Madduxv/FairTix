package com.fairtix.venues.infrastructure;

import com.fairtix.venues.domain.VenueSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenueSectionRepository extends JpaRepository<VenueSection, UUID> {

    List<VenueSection> findByVenue_Id(UUID venueId);

    boolean existsByVenue_IdAndName(UUID venueId, String name);
}
