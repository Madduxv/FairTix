package com.fairtix.fairtix.inventory.infrastructure;

import com.fairtix.fairtix.inventory.domain.Seat;
import com.fairtix.fairtix.inventory.domain.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByEvent_Id(UUID eventId);

    List<Seat> findByEvent_IdAndStatus(UUID eventId, SeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = ?1")
    Optional<Seat> findAndLockById(UUID id);
}
