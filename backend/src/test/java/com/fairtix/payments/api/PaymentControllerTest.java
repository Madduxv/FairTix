package com.fairtix.payments.api;

import com.fairtix.auth.WithMockPrincipal;
import com.fairtix.events.domain.Event;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class PaymentControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private UserRepository userRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private SeatHoldRepository seatHoldRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private MockMvc mockMvc;
  private User user;
  private SeatHold confirmedHold;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    user = new User();
    user.setEmail("paytest@test.com");
    user.setPassword(passwordEncoder.encode("Test1234!"));
    user = userRepository.save(user);

    Event event = new Event("Test Event", "Test Venue", Instant.now().plusSeconds(86400 * 30), null);
    event = eventRepository.save(event);

    Seat seat = new Seat(event, "A", "1", "1", new java.math.BigDecimal("25.00"));
    seat.setStatus(SeatStatus.BOOKED);
    seat = seatRepository.save(seat);

    confirmedHold = new SeatHold(seat, user.getId(), Instant.now().plusSeconds(600));
    confirmedHold.setStatus(HoldStatus.CONFIRMED);
    confirmedHold = seatHoldRepository.save(confirmedHold);
  }

  @Test
  void checkout_success_returnsCreated() throws Exception {
    String body = """
        {
          "holdIds": ["%s"],
          "simulatedOutcome": "SUCCESS"
        }
        """.formatted(confirmedHold.getId());

    mockMvc.perform(post("/api/payments/checkout")
            .with(WithMockPrincipal.user(user.getId(), user.getEmail()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
        .andExpect(jsonPath("$.orderStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.transactionId").isNotEmpty())
        .andExpect(jsonPath("$.amount").value(25.00));
  }

  @Test
  void checkout_failure_returns402() throws Exception {
    String body = """
        {
          "holdIds": ["%s"],
          "simulatedOutcome": "FAILURE"
        }
        """.formatted(confirmedHold.getId());

    mockMvc.perform(post("/api/payments/checkout")
            .with(WithMockPrincipal.user(user.getId(), user.getEmail()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isPaymentRequired());
  }

  @Test
  void checkout_cancelled_returns402() throws Exception {
    String body = """
        {
          "holdIds": ["%s"],
          "simulatedOutcome": "CANCELLED"
        }
        """.formatted(confirmedHold.getId());

    mockMvc.perform(post("/api/payments/checkout")
            .with(WithMockPrincipal.user(user.getId(), user.getEmail()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isPaymentRequired());
  }

  @Test
  void checkout_unauthenticated_returns401or403() throws Exception {
    String body = """
        {
          "holdIds": ["%s"],
          "simulatedOutcome": "SUCCESS"
        }
        """.formatted(confirmedHold.getId());

    mockMvc.perform(post("/api/payments/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void checkout_emptyHoldIds_returns400() throws Exception {
    String body = """
        {
          "holdIds": [],
          "simulatedOutcome": "SUCCESS"
        }
        """;

    mockMvc.perform(post("/api/payments/checkout")
            .with(WithMockPrincipal.user(user.getId(), user.getEmail()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }
}
