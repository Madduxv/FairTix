package com.fairtix.performers.infrastructure;

import com.fairtix.performers.domain.Performer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PerformerRepository extends JpaRepository<Performer, UUID> {
  Optional<Performer> findByNameIgnoreCase(String name);
  boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

  @Modifying
  @Query(value = "DELETE FROM event_performers WHERE performer_id = :performerId", nativeQuery = true)
  void deleteEventAssociations(@Param("performerId") UUID performerId);
}
