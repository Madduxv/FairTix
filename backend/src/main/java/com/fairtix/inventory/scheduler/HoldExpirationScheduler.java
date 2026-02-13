package com.fairtix.inventory.scheduler;

import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically scans for active holds whose expiry has passed and marks them
 * EXPIRED, returning the corresponding seats to AVAILABLE.
 *
 * Interval is controlled via {@code holds.cleanup.interval-ms} (default 30 s).
 */
@Component
public class HoldExpirationScheduler {

  private final SeatHoldRepository seatHoldRepository;
  private final SeatRepository seatRepository;

  public HoldExpirationScheduler(SeatHoldRepository seatHoldRepository,
      SeatRepository seatRepository) {
    this.seatHoldRepository = seatHoldRepository;
    this.seatRepository = seatRepository;
  }

  @Scheduled(fixedDelayString = "${holds.cleanup.interval-ms:30000}")
  @Transactional
  public void expireHolds() {
    List<SeatHold> expired = seatHoldRepository
        .findAllByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, Instant.now());

    for (SeatHold hold : expired) {
      hold.setStatus(HoldStatus.EXPIRED);
      seatHoldRepository.save(hold);

      Seat seat = hold.getSeat();
      if (seat.getStatus() == SeatStatus.HELD) {
        seat.setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
      }
    }
  }
}
