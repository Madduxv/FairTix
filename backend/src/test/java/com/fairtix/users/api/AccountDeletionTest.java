package com.fairtix.users.api;

import com.fairtix.auth.WithMockPrincipal;
import com.fairtix.events.domain.Event;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.users.domain.Role;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class AccountDeletionTest {

  @Autowired private WebApplicationContext context;
  @Autowired private UserRepository userRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private SeatHoldRepository seatHoldRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private MockMvc mockMvc;
  private User regularUser;
  private User adminUser;
  private Event event;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    regularUser = new User();
    regularUser.setEmail("deluser@test.com");
    regularUser.setPassword(passwordEncoder.encode("Test1234!"));
    regularUser = userRepository.save(regularUser);

    adminUser = new User();
    adminUser.setEmail("admin@test.com");
    adminUser.setPassword(passwordEncoder.encode("Admin1234!"));
    adminUser.setRole(Role.ADMIN);
    adminUser = userRepository.save(adminUser);

    event = new Event("Test Event", "Test Venue", Instant.now().plusSeconds(86400 * 30), null);
    event = eventRepository.save(event);
  }

  // -------------------------------------------------------------------------
  // Self-deletion: DELETE /api/users/me
  // -------------------------------------------------------------------------

  @Test
  void deleteOwnAccount_success() throws Exception {
    mockMvc.perform(delete("/api/users/me")
            .with(WithMockPrincipal.user(regularUser.getId(), regularUser.getEmail())))
        .andExpect(status().isNoContent());

    User deleted = userRepository.findById(regularUser.getId()).orElseThrow();
    assertTrue(deleted.isDeleted());
    assertTrue(deleted.getEmail().startsWith("deleted_"));
    assertEquals("DELETED", deleted.getPassword());
  }

  @Test
  void deleteOwnAccount_releasesActiveHolds() throws Exception {
    Seat seat = new Seat(event, "A", "1", "1", new java.math.BigDecimal("25.00"));
    seat.setStatus(SeatStatus.HELD);
    seat = seatRepository.save(seat);

    SeatHold hold = new SeatHold(seat, regularUser.getId(), Instant.now().plusSeconds(600));
    hold = seatHoldRepository.save(hold);
    UUID holdId = hold.getId();
    UUID seatId = seat.getId();

    mockMvc.perform(delete("/api/users/me")
            .with(WithMockPrincipal.user(regularUser.getId(), regularUser.getEmail())))
        .andExpect(status().isNoContent());

    SeatHold releasedHold = seatHoldRepository.findById(holdId).orElseThrow();
    assertEquals(HoldStatus.RELEASED, releasedHold.getStatus());

    Seat releasedSeat = seatRepository.findById(seatId).orElseThrow();
    assertEquals(SeatStatus.AVAILABLE, releasedSeat.getStatus());
  }

  @Test
  void deleteOwnAccount_unauthenticated_returns403() throws Exception {
    mockMvc.perform(delete("/api/users/me"))
        .andExpect(status().isForbidden());
  }

  @Test
  void deletedUser_cannotLogin() throws Exception {
    // Delete the account
    mockMvc.perform(delete("/api/users/me")
            .with(WithMockPrincipal.user(regularUser.getId(), regularUser.getEmail())))
        .andExpect(status().isNoContent());

    // Try to login
    String loginBody = """
        { "email": "deluser@test.com", "password": "Test1234!" }
        """;
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isUnauthorized());
  }

  // -------------------------------------------------------------------------
  // Admin deletion: DELETE /api/admin/users/{id}
  // -------------------------------------------------------------------------

  @Test
  void adminDeleteUser_success() throws Exception {
    mockMvc.perform(delete("/api/admin/users/" + regularUser.getId())
            .with(WithMockPrincipal.admin(adminUser.getId(), adminUser.getEmail())))
        .andExpect(status().isNoContent());

    User deleted = userRepository.findById(regularUser.getId()).orElseThrow();
    assertTrue(deleted.isDeleted());
  }

  @Test
  void adminCannotDeleteSelf() throws Exception {
    mockMvc.perform(delete("/api/admin/users/" + adminUser.getId())
            .with(WithMockPrincipal.admin(adminUser.getId(), adminUser.getEmail())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nonAdminCannotDeleteOtherUsers() throws Exception {
    mockMvc.perform(delete("/api/admin/users/" + adminUser.getId())
            .with(WithMockPrincipal.user(regularUser.getId(), regularUser.getEmail())))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminDeleteNonExistentUser_returns404() throws Exception {
    mockMvc.perform(delete("/api/admin/users/" + UUID.randomUUID())
            .with(WithMockPrincipal.admin(adminUser.getId(), adminUser.getEmail())))
        .andExpect(status().isNotFound());
  }
}
