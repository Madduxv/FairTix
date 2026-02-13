package com.fairtix.fairtix.inventory.application;

import com.fairtix.fairtix.events.infrastructure.EventRepository;
import com.fairtix.fairtix.inventory.domain.HoldStatus;
import com.fairtix.fairtix.inventory.domain.Seat;
import com.fairtix.fairtix.inventory.domain.SeatHold;
import com.fairtix.fairtix.inventory.domain.SeatStatus;
import com.fairtix.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.fairtix.inventory.infrastructure.SeatRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SeatHoldService {

    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final EventRepository eventRepository;

    @Value("${holds.duration-minutes:10}")
    private int defaultDurationMinutes;

    public SeatHoldService(SeatRepository seatRepository,
                           SeatHoldRepository seatHoldRepository,
                           EventRepository eventRepository) {
        this.seatRepository = seatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Places a hold on one or more seats for an event.
     * All seats are held atomically — if any seat is unavailable the entire
     * operation is rolled back and a {@link SeatHoldConflictException} is thrown.
     *
     * @param eventId         the target event
     * @param seatIds         the seats to hold
     * @param holderId        an opaque identifier for the holder (session/user id)
     * @param durationMinutes how long the hold should last; falls back to
     *                        {@code holds.duration-minutes} if null/non-positive
     * @return the created {@link SeatHold} records
     */
    @Transactional
    public List<SeatHold> createHold(UUID eventId, List<UUID> seatIds, String holderId, Integer durationMinutes) {
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        int duration = (durationMinutes != null && durationMinutes > 0)
                ? durationMinutes
                : defaultDurationMinutes;
        Instant expiresAt = Instant.now().plusSeconds(duration * 60L);

        List<SeatHold> holds = new ArrayList<>();
        for (UUID seatId : seatIds) {
            // Pessimistic write lock prevents concurrent holds on the same seat
            Seat seat = seatRepository.findAndLockById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

            if (!seat.getEvent().getId().equals(eventId)) {
                throw new IllegalArgumentException(
                        "Seat " + seatId + " does not belong to event " + eventId);
            }

            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatHoldConflictException(
                        "Seat " + seatId + " is not available (status: " + seat.getStatus() + ")");
            }

            seat.setStatus(SeatStatus.HELD);
            seatRepository.save(seat);

            holds.add(seatHoldRepository.save(new SeatHold(seat, holderId, expiresAt)));
        }
        return holds;
    }

    /**
     * Releases an active hold, returning the seat to AVAILABLE.
     *
     * @param holdId   the hold to release
     * @param holderId must match the holder who created the hold
     * @return the updated {@link SeatHold}
     */
    @Transactional
    public SeatHold releaseHold(UUID holdId, String holderId) {
        SeatHold hold = seatHoldRepository.findByIdAndHolderId(holdId, holderId)
                .orElseThrow(() -> new SeatHoldNotFoundException("Hold not found: " + holdId));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new SeatHoldConflictException(
                    "Hold is not active (status: " + hold.getStatus() + ")");
        }

        hold.setStatus(HoldStatus.RELEASED);
        Seat seat = hold.getSeat();
        if (seat.getStatus() == SeatStatus.HELD) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }
        return seatHoldRepository.save(hold);
    }

    /**
     * Confirms an active hold, transitioning the seat to BOOKED.
     *
     * @param holdId   the hold to confirm
     * @param holderId must match the holder who created the hold
     * @return the updated {@link SeatHold}
     */
    @Transactional
    public SeatHold confirmHold(UUID holdId, String holderId) {
        SeatHold hold = seatHoldRepository.findByIdAndHolderId(holdId, holderId)
                .orElseThrow(() -> new SeatHoldNotFoundException("Hold not found: " + holdId));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new SeatHoldConflictException(
                    "Hold is not active (status: " + hold.getStatus() + ")");
        }

        if (hold.getExpiresAt().isBefore(Instant.now())) {
            throw new SeatHoldConflictException("Hold has expired");
        }

        hold.setStatus(HoldStatus.CONFIRMED);
        Seat seat = hold.getSeat();
        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);
        return seatHoldRepository.save(hold);
    }

    /**
     * Returns hold details for the given holder.
     *
     * @param holdId   the hold id
     * @param holderId must match the holder who created the hold
     * @return the {@link SeatHold}
     */
    @Transactional
    public SeatHold getHold(UUID holdId, String holderId) {
        return seatHoldRepository.findByIdAndHolderId(holdId, holderId)
                .orElseThrow(() -> new SeatHoldNotFoundException("Hold not found: " + holdId));
    }
}
