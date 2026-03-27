package com.fairtix.inventory.infrastructure;

import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

  @Query("SELECT s.status, COUNT(s) FROM Seat s GROUP BY s.status")
  List<Object[]> countByStatusGrouped();

  @Query("SELECT s.event.id, s.event.title, s.status, COUNT(s) FROM Seat s GROUP BY s.event.id, s.event.title, s.status")
  List<Object[]> countByEventAndStatus();

  List<Seat> findByEvent_Id(UUID eventId);

  List<Seat> findByEvent_IdAndStatus(UUID eventId, SeatStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Seat s WHERE s.id = ?1")
  Optional<Seat> findAndLockById(UUID id);

  /**
   * Acquires pessimistic write locks on multiple seats in a single round trip.
   *
   * <p>The {@code ORDER BY s.id} clause ensures all transactions lock seats in
   * the same UUID order, which eliminates the circular-wait condition that
   * causes deadlocks when two requests hold disjoint seat sets.
   *
   * <p>Always sort the input {@code ids} list in Java as well before calling
   * this method, so the JPQL ordering and the application ordering match.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
  List<Seat> findAndLockByIdIn(@Param("ids") List<UUID> ids);
}
