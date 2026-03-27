package com.fairtix.events.infrastructure;

import com.fairtix.events.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>,
    JpaSpecificationExecutor<Event> {

  long countByStartTimeAfter(Instant now);

  @Query("SELECT e.venue, COUNT(e) FROM Event e GROUP BY e.venue")
  List<Object[]> countByVenueGrouped();
}
