package com.fairtix.inventory.application;

import com.fairtix.events.domain.Event;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.inventory.scheduler.HoldExpirationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SeatHoldServiceTest {

  @Autowired private SeatHoldService seatHoldService;
  @Autowired private SeatRepository seatRepository;
  @Autowired private SeatHoldRepository seatHoldRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private HoldExpirationScheduler scheduler;

  private Event testEvent;
  private Seat testSeat;

  @BeforeEach
  void setUp() {
    testEvent = eventRepository.save(
        new Event("Test Event", "Test Venue", Instant.now().plusSeconds(3600)));
    testSeat = seatRepository.save(new Seat(testEvent, "Floor", "A", "101"));
  }

  // -------------------------------------------------------------------------
  // Original tests (kept intact)
  // -------------------------------------------------------------------------

  @Test
  void holdingASeatSucceeds() {
    List<SeatHold> holds = seatHoldService.createHold(
        testEvent.getId(), List.of(testSeat.getId()), "user-1", 10);

    assertThat(holds).hasSize(1);
    SeatHold hold = holds.get(0);
    assertThat(hold.getStatus()).isEqualTo(HoldStatus.ACTIVE);
    assertThat(hold.getHolderId()).isEqualTo("user-1");
    assertThat(hold.getExpiresAt()).isAfter(Instant.now());

    Seat updatedSeat = seatRepository.findById(testSeat.getId()).orElseThrow();
    assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
  }

  @Test
  void holdingTheSameSeatTwiceReturnsConflict() {
    seatHoldService.createHold(
        testEvent.getId(), List.of(testSeat.getId()), "user-1", 10);

    assertThatThrownBy(() -> seatHoldService.createHold(
        testEvent.getId(), List.of(testSeat.getId()), "user-2", 10))
        .isInstanceOf(SeatHoldConflictException.class)
        .hasMessageContaining("not available");
  }

  @Test
  void expiredHoldsGetReleasedByScheduler() {
    testSeat.setStatus(SeatStatus.HELD);
    seatRepository.save(testSeat);

    SeatHold expiredHold = new SeatHold(testSeat, "user-1", Instant.now().minusSeconds(60));
    seatHoldRepository.save(expiredHold);

    scheduler.expireHolds();

    SeatHold hold = seatHoldRepository.findById(expiredHold.getId()).orElseThrow();
    assertThat(hold.getStatus()).isEqualTo(HoldStatus.EXPIRED);

    Seat seat = seatRepository.findById(testSeat.getId()).orElseThrow();
    assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
  }

  // -------------------------------------------------------------------------
  // Idempotency
  // -------------------------------------------------------------------------

  @Test
  void releasingAnAlreadyReleasedHoldIsIdempotent() {
    SeatHold hold = seatHoldService.createHold(
        testEvent.getId(), List.of(testSeat.getId()), "user-1", 10).get(0);

    SeatHold firstRelease  = seatHoldService.releaseHold(hold.getId(), "user-1");
    SeatHold secondRelease = seatHoldService.releaseHold(hold.getId(), "user-1");

    assertThat(firstRelease.getStatus()).isEqualTo(HoldStatus.RELEASED);
    assertThat(secondRelease.getStatus()).isEqualTo(HoldStatus.RELEASED);
    // Seat must remain AVAILABLE after idempotent call
    Seat seat = seatRepository.findById(testSeat.getId()).orElseThrow();
    assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
  }

  @Test
  void confirmingAnAlreadyConfirmedHoldIsIdempotent() {
    SeatHold hold = seatHoldService.createHold(
        testEvent.getId(), List.of(testSeat.getId()), "user-1", 10).get(0);

    SeatHold firstConfirm  = seatHoldService.confirmHold(hold.getId(), "user-1");
    SeatHold secondConfirm = seatHoldService.confirmHold(hold.getId(), "user-1");

    assertThat(firstConfirm.getStatus()).isEqualTo(HoldStatus.CONFIRMED);
    assertThat(secondConfirm.getStatus()).isEqualTo(HoldStatus.CONFIRMED);
    Seat seat = seatRepository.findById(testSeat.getId()).orElseThrow();
    assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
  }

  // -------------------------------------------------------------------------
  // Duration clamping (max-duration-minutes=60 in test properties)
  // -------------------------------------------------------------------------

  @Test
  void durationIsClampedToMaxDurationMinutes() {
    // Request a hold 10× longer than the allowed maximum
    List<SeatHold> holds = seatHoldService.createHold(
        testEvent.getId(), List.of(testSeat.getId()), "user-1", 600);

    Instant hold60MinBound = Instant.now().plusSeconds(60 * 60L + 5); // 60 min + 5 s slack
    assertThat(holds.get(0).getExpiresAt()).isBefore(hold60MinBound);
  }

  // -------------------------------------------------------------------------
  // Max active holds per holder (max-active-per-holder=2 in test properties)
  // -------------------------------------------------------------------------

  @Test
  void exceedingMaxActiveHoldsPerHolderThrowsConflict() {
    // Create 2 seats so "user-1" can reach the limit
    Seat extraSeat = seatRepository.save(new Seat(testEvent, "Floor", "A", "102"));

    seatHoldService.createHold(testEvent.getId(), List.of(testSeat.getId()), "user-1", 10);
    seatHoldService.createHold(testEvent.getId(), List.of(extraSeat.getId()), "user-1", 10);

    // Third seat — must be blocked by the active-hold limit
    Seat thirdSeat = seatRepository.save(new Seat(testEvent, "Floor", "A", "103"));
    assertThatThrownBy(() -> seatHoldService.createHold(
        testEvent.getId(), List.of(thirdSeat.getId()), "user-1", 10))
        .isInstanceOf(SeatHoldConflictException.class)
        .hasMessageContaining("Hold limit reached");
  }

  @Test
  void holdLimitIsPerHolder_otherHolderCanStillHold() {
    Seat extraSeat = seatRepository.save(new Seat(testEvent, "Floor", "A", "102"));

    seatHoldService.createHold(testEvent.getId(), List.of(testSeat.getId()), "user-1", 10);
    seatHoldService.createHold(testEvent.getId(), List.of(extraSeat.getId()), "user-1", 10);

    // user-2 is unaffected by user-1's limit
    Seat thirdSeat = seatRepository.save(new Seat(testEvent, "Floor", "A", "103"));
    List<SeatHold> holds = seatHoldService.createHold(
        testEvent.getId(), List.of(thirdSeat.getId()), "user-2", 10);
    assertThat(holds).hasSize(1);
    assertThat(holds.get(0).getStatus()).isEqualTo(HoldStatus.ACTIVE);
  }

  // -------------------------------------------------------------------------
  // Batch locking / seat ordering
  // -------------------------------------------------------------------------

  @Test
  void batchHoldLocksSeatsInAnyInputOrder() {
    // Create two seats and request them in reverse UUID order.
    // The service must sort them internally; both must end up HELD.
    Seat seat2 = seatRepository.save(new Seat(testEvent, "Floor", "A", "102"));

    // Provide seatIds in descending order (likely reversed from UUID sort)
    UUID id1 = testSeat.getId();
    UUID id2 = seat2.getId();
    List<UUID> reversed = id1.compareTo(id2) > 0
        ? List.of(id1, id2)
        : List.of(id2, id1);

    List<SeatHold> holds = seatHoldService.createHold(
        testEvent.getId(), reversed, "user-1", 10);

    assertThat(holds).hasSize(2);
    assertThat(seatRepository.findById(id1).orElseThrow().getStatus()).isEqualTo(SeatStatus.HELD);
    assertThat(seatRepository.findById(id2).orElseThrow().getStatus()).isEqualTo(SeatStatus.HELD);
  }

  @Test
  void duplicateSeatIdsInRequestAreDeduped() {
    // Sending the same seat twice should create exactly one hold
    List<SeatHold> holds = seatHoldService.createHold(
        testEvent.getId(),
        List.of(testSeat.getId(), testSeat.getId()),
        "user-1", 10);

    assertThat(holds).hasSize(1);
  }

  // -------------------------------------------------------------------------
  // Scheduler with paging
  // -------------------------------------------------------------------------

  @Test
  void schedulerExpiresMultipleHoldsInOneBatch() {
    // Create 3 expired holds manually (scheduler page size is 500, so all fit)
    Seat seat2 = seatRepository.save(new Seat(testEvent, "Floor", "A", "102"));
    Seat seat3 = seatRepository.save(new Seat(testEvent, "Floor", "A", "103"));

    for (Seat seat : List.of(testSeat, seat2, seat3)) {
      seat.setStatus(SeatStatus.HELD);
      seatRepository.save(seat);
      seatHoldRepository.save(new SeatHold(seat, "user-1", Instant.now().minusSeconds(60)));
    }

    scheduler.expireHolds();

    // All 3 holds must be EXPIRED, all seats back to AVAILABLE
    List<SeatHold> holds = seatHoldRepository.findAll();
    assertThat(holds).allMatch(h -> h.getStatus() == HoldStatus.EXPIRED);
    assertThat(seatRepository.findById(testSeat.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.AVAILABLE);
    assertThat(seatRepository.findById(seat2.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.AVAILABLE);
    assertThat(seatRepository.findById(seat3.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.AVAILABLE);
  }

  // -------------------------------------------------------------------------
  // List holds
  // -------------------------------------------------------------------------

  @Test
  void listHoldsReturnsOnlyActiveHoldsForHolder() {
    Seat seat2 = seatRepository.save(new Seat(testEvent, "Floor", "A", "102"));

    // Two holds for user-1 (hits the limit of 2 for test props)
    seatHoldService.createHold(testEvent.getId(), List.of(testSeat.getId()), "user-1", 10);
    seatHoldService.createHold(testEvent.getId(), List.of(seat2.getId()),    "user-1", 10);

    List<SeatHold> active = seatHoldService.listHolds("user-1", HoldStatus.ACTIVE);
    assertThat(active).hasSize(2);
    assertThat(active).allMatch(h -> h.getStatus() == HoldStatus.ACTIVE);
    assertThat(active).allMatch(h -> h.getHolderId().equals("user-1"));
  }
}
