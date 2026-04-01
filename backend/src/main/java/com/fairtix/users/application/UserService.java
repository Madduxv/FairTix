package com.fairtix.users.application;

import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final SeatHoldRepository seatHoldRepository;
  private final SeatRepository seatRepository;

  public UserService(UserRepository userRepository,
      SeatHoldRepository seatHoldRepository,
      SeatRepository seatRepository) {
    this.userRepository = userRepository;
    this.seatHoldRepository = seatHoldRepository;
    this.seatRepository = seatRepository;
  }

  /**
   * Soft-deletes a user account by anonymizing PII and releasing active holds.
   * Preserves referential integrity with orders and tickets.
   */
  @Transactional
  public void deleteAccount(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (user.isDeleted()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    releaseUserHolds(userId);
    anonymizeUser(user);
  }

  /**
   * Admin-initiated deletion of any user account.
   */
  @Transactional
  public void adminDeleteAccount(UUID adminId, UUID targetUserId) {
    if (adminId.equals(targetUserId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Admins cannot delete their own account via the admin endpoint");
    }

    User target = userRepository.findById(targetUserId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (target.isDeleted()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    releaseUserHolds(targetUserId);
    anonymizeUser(target);
  }

  private void releaseUserHolds(UUID userId) {
    // Release ACTIVE holds
    List<SeatHold> activeHolds = seatHoldRepository.findAllByOwnerIdAndStatus(
        userId, HoldStatus.ACTIVE);
    for (SeatHold hold : activeHolds) {
      hold.setStatus(HoldStatus.RELEASED);
      hold.getSeat().setStatus(SeatStatus.AVAILABLE);
      seatRepository.save(hold.getSeat());
    }
    seatHoldRepository.saveAll(activeHolds);

    // Release CONFIRMED holds — only revert seats that aren't already SOLD
    List<SeatHold> confirmedHolds = seatHoldRepository.findAllByOwnerIdAndStatus(
        userId, HoldStatus.CONFIRMED);
    for (SeatHold hold : confirmedHolds) {
      hold.setStatus(HoldStatus.RELEASED);
      if (hold.getSeat().getStatus() != SeatStatus.SOLD) {
        hold.getSeat().setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(hold.getSeat());
      }
    }
    seatHoldRepository.saveAll(confirmedHolds);
  }

  private void anonymizeUser(User user) {
    user.setEmail("deleted_" + user.getId() + "@fairtix.local");
    user.setPassword("DELETED");
    user.setDeletedAt(Instant.now());
    userRepository.save(user);
  }
}
