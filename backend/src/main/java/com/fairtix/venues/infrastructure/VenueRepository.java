package com.fairtix.venues.infrastructure;

import com.fairtix.venues.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

  Optional<Venue> findByName(String name);

  boolean existsByNameAndIdNot(String name, UUID id);

  @Query(nativeQuery = true, value = """
      SELECT v.id AS venueId,
             (6371 * acos(
               GREATEST(-1.0, LEAST(1.0,
                 cos(radians(:lat)) * cos(radians(v.latitude)) *
                 cos(radians(v.longitude) - radians(:lon)) +
                 sin(radians(:lat)) * sin(radians(v.latitude))
               ))
             )) AS distanceKm
      FROM venues v
      WHERE v.latitude IS NOT NULL AND v.longitude IS NOT NULL
        AND (6371 * acos(
               GREATEST(-1.0, LEAST(1.0,
                 cos(radians(:lat)) * cos(radians(v.latitude)) *
                 cos(radians(v.longitude) - radians(:lon)) +
                 sin(radians(:lat)) * sin(radians(v.latitude))
               ))
             )) <= :radiusKm
      ORDER BY distanceKm ASC
      """)
  List<VenueDistance> findVenuesWithinRadius(
      @Param("lat") double lat,
      @Param("lon") double lon,
      @Param("radiusKm") double radiusKm);
}
