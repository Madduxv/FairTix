package com.fairtix.fairtix.inventory.application;

import com.fairtix.fairtix.events.domain.Event;
import com.fairtix.fairtix.events.infrastructure.EventRepository;
import com.fairtix.fairtix.inventory.domain.HoldStatus;
import com.fairtix.fairtix.inventory.domain.Seat;
import com.fairtix.fairtix.inventory.domain.SeatHold;
import com.fairtix.fairtix.inventory.domain.SeatStatus;
import com.fairtix.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.fairtix.inventory.scheduler.HoldExpirationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SeatHoldServiceTest {

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private HoldExpirationScheduler scheduler;

    private Event testEvent;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        testEvent = eventRepository.save(
                new Event("Test Event", "Test Venue", Instant.now().plusSeconds(3600)));
        testSeat = seatRepository.save(
                new Seat(testEvent, "Floor", "A", "101"));
    }

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

        assertThatThrownBy(() ->
                seatHoldService.createHold(
                        testEvent.getId(), List.of(testSeat.getId()), "user-2", 10))
                .isInstanceOf(SeatHoldConflictException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void expiredHoldsGetReleasedByScheduler() {
        // Manually create an already-expired hold (bypassing service duration logic)
        testSeat.setStatus(SeatStatus.HELD);
        seatRepository.save(testSeat);

        SeatHold expiredHold = new SeatHold(testSeat, "user-1", Instant.now().minusSeconds(60));
        seatHoldRepository.save(expiredHold);

        // Invoke scheduler directly within the same transaction so it sees the data
        scheduler.expireHolds();

        SeatHold hold = seatHoldRepository.findById(expiredHold.getId()).orElseThrow();
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.EXPIRED);

        Seat seat = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}
