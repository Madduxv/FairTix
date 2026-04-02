package com.fairtix.inventory.api;

import com.fairtix.events.application.EventService;
import com.fairtix.events.domain.Event;
import com.fairtix.inventory.application.SeatService;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SeatController}.
 *
 * <p>POST (create seat) requires ADMIN role. GET (list seats) is permitAll.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class SeatControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private EventService eventService;

  @Autowired
  private SeatService seatService;

  private MockMvc mockMvc;
  private Event testEvent;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
    testEvent = eventService.createEvent("Seat Test Event", Instant.parse("2026-08-01T19:00:00Z"), "Arena");
  }

  // -------------------------------------------------------------------------
  // POST /api/events/{eventId}/seats — create seat
  // -------------------------------------------------------------------------

  @Test
  void createSeat_asAdmin_returns201() throws Exception {
    String body = """
        {
          "section":    "Floor",
          "rowLabel":   "A",
          "seatNumber": "101",
          "price":      49.99
        }
        """;

    mockMvc.perform(post("/api/events/{eventId}/seats", testEvent.getId())
            .with(user("admin@test.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.eventId").value(testEvent.getId().toString()))
        .andExpect(jsonPath("$.section").value("Floor"))
        .andExpect(jsonPath("$.rowLabel").value("A"))
        .andExpect(jsonPath("$.seatNumber").value("101"))
        .andExpect(jsonPath("$.price").value(49.99))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
  }

  @Test
  void createSeat_asUser_returns403() throws Exception {
    String body = """
        {
          "section":    "Floor",
          "rowLabel":   "A",
          "seatNumber": "101",
          "price":      49.99
        }
        """;

    mockMvc.perform(post("/api/events/{eventId}/seats", testEvent.getId())
            .with(user("user@test.com").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void createSeat_unauthenticated_returns403() throws Exception {
    String body = """
        {
          "section":    "Floor",
          "rowLabel":   "A",
          "seatNumber": "101",
          "price":      49.99
        }
        """;

    mockMvc.perform(post("/api/events/{eventId}/seats", testEvent.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void createSeat_nonExistentEvent_returns400() throws Exception {
    String body = """
        {
          "section":    "Floor",
          "rowLabel":   "A",
          "seatNumber": "101",
          "price":      49.99
        }
        """;

    mockMvc.perform(post("/api/events/{eventId}/seats", UUID.randomUUID())
            .with(user("admin@test.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }

  // -------------------------------------------------------------------------
  // GET /api/events/{eventId}/seats — list seats (permitAll)
  // -------------------------------------------------------------------------

  @Test
  void getSeats_returnsAllSeats() throws Exception {
    seatService.createSeat(testEvent.getId(), "Floor", "A", "1", new BigDecimal("25.00"));
    seatService.createSeat(testEvent.getId(), "Floor", "A", "2", new BigDecimal("25.00"));
    seatService.createSeat(testEvent.getId(), "Balcony", "B", "1", new BigDecimal("15.00"));

    mockMvc.perform(get("/api/events/{eventId}/seats", testEvent.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  void getSeats_emptyEvent_returnsEmptyList() throws Exception {
    mockMvc.perform(get("/api/events/{eventId}/seats", testEvent.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void getSeats_availableOnly_filtersCorrectly() throws Exception {
    Seat seat1 = seatService.createSeat(testEvent.getId(), "Floor", "A", "1", new BigDecimal("25.00"));
    seatService.createSeat(testEvent.getId(), "Floor", "A", "2", new BigDecimal("25.00"));

    // Mark seat1 as held to make it unavailable
    // Entity is managed within @Transactional so the change is auto-flushed
    seat1.setStatus(SeatStatus.HELD);

    mockMvc.perform(get("/api/events/{eventId}/seats", testEvent.getId())
            .param("availableOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].seatNumber").value("2"));
  }
}
