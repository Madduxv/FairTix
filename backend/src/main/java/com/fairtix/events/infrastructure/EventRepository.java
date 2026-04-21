package com.fairtix.events.infrastructure;

import com.fairtix.events.domain.Event;
import com.fairtix.venues.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>,
    JpaSpecificationExecutor<Event> {

  long countByStartTimeAfter(Instant now);

  boolean existsByVenue_Id(UUID venueId);

  @Query("SELECT e.venue.name, COUNT(e) FROM Event e GROUP BY e.venue.name")
  List<Object[]> countByVenueGrouped();

  Optional<Event> findByTitleAndStartTimeAndVenue(
      String title,
      Instant startTime,
      Venue venue);
}
