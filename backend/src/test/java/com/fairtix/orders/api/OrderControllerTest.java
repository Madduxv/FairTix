package com.fairtix.orders.api;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class OrderControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private SeatRepository seatRepository;

  @Autowired
  private SeatHoldRepository seatHoldRepository;

  private MockMvc mockMvc;
  private User testUser;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    testUser = new User();
    testUser.setEmail("ordertest@example.com");
    testUser.setPassword("$2a$10$dummyhashfortest");
    testUser = userRepository.save(testUser);
  }

  @Test
  void createOrder_unauthenticated_returns403() throws Exception {
    String body = """
        { "holdIds": ["00000000-0000-0000-0000-000000000001"] }
        """;

    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "ordertest@example.com")
  void createOrder_emptyHoldIds_returns400() throws Exception {
    String body = """
        { "holdIds": [] }
        """;

    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(username = "ordertest@example.com")
  void createOrder_nonExistentHold_returns400() throws Exception {
    String body = """
        { "holdIds": ["00000000-0000-0000-0000-000000000001"] }
        """;

    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  @WithMockUser(username = "ordertest@example.com")
  void createOrder_withConfirmedHold_returns201() throws Exception {
    // Set up: event → seat → confirmed hold
    Event event = eventRepository.save(new Event("Test Concert", "Test Venue", Instant.now().plusSeconds(86400)));
    Seat seat = seatRepository.save(new Seat(event, "A", "1", "101"));
    seat.setStatus(SeatStatus.BOOKED);
    seat = seatRepository.save(seat);

    SeatHold hold = new SeatHold(seat, testUser.getId().toString(), Instant.now().plusSeconds(600));
    hold.setStatus(HoldStatus.CONFIRMED);
    hold = seatHoldRepository.save(hold);

    String body = """
        { "holdIds": ["%s"] }
        """.formatted(hold.getId());

    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  @WithMockUser(username = "ordertest@example.com")
  void listOrders_returnsEmptyWhenNone() throws Exception {
    mockMvc.perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void listOrders_unauthenticated_returns403() throws Exception {
    mockMvc.perform(get("/api/orders"))
        .andExpect(status().isForbidden());
  }
}
