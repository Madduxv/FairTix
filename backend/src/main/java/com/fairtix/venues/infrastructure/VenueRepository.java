package com.fairtix.venues.infrastructure;

import com.fairtix.venues.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID>,
        JpaSpecificationExecutor<Venue> {
}
