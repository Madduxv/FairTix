package com.fairtix.events.infrastructure;

import com.fairtix.events.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>,
        JpaSpecificationExecutor<Event> {
}
