package com.fairtix.inventory.infrastructure;

import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

  Optional<SeatHold> findByIdAndHolderId(UUID id, String holderId);

  Optional<SeatHold> findBySeat_IdAndStatus(UUID seatId, HoldStatus status);

  List<SeatHold> findAllByStatusAndExpiresAtBefore(HoldStatus status, Instant now);
}
