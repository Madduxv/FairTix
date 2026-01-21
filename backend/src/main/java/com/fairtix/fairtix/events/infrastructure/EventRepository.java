package com.fairtix.fairtix.events.infrastructure;

import com.fairtix.fairtix.events.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
}
