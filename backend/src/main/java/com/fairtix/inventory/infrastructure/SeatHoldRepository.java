package com.fairtix.inventory.infrastructure;

import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.SeatHold;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

  @Query("SELECT sh.status, COUNT(sh) FROM SeatHold sh GROUP BY sh.status")
  List<Object[]> countByStatusGrouped();

  @Query(value = "SELECT CAST(sh.created_at AS DATE) AS hold_date, COUNT(*) FROM seat_holds sh WHERE sh.created_at >= :since GROUP BY CAST(sh.created_at AS DATE) ORDER BY hold_date", nativeQuery = true)
  List<Object[]> countHoldsPerDay(@Param("since") Instant since);

  Optional<SeatHold> findByIdAndHolderId(UUID id, String holderId);

  Optional<SeatHold> findBySeat_IdAndStatus(UUID seatId, HoldStatus status);

  /**
   * Used by the scheduler to fetch one bounded page of expired holds.
   * Always query page 0: expired items are mutated to EXPIRED each run,
   * so they naturally disappear from subsequent queries.
   */
  Page<SeatHold> findAllByStatusAndExpiresAtBefore(HoldStatus status, Instant now, Pageable pageable);

  /** Soft-limit: count how many active holds a holder currently has. */
  long countByHolderIdAndStatus(String holderId, HoldStatus status);

  /** List-holds endpoint: all holds for a holder filtered by status. */
  List<SeatHold> findAllByHolderIdAndStatus(String holderId, HoldStatus status);
}
