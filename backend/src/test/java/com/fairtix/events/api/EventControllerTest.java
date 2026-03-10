package com.fairtix.events.api;

import com.fairtix.events.application.EventService;
import com.fairtix.events.domain.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link EventController}.
 *
 * <p>GET endpoints are permitAll. POST requires ADMIN role.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class EventControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private EventService eventService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // -------------------------------------------------------------------------
  // POST /api/events — create event
  // -------------------------------------------------------------------------

  @Test
  void createEvent_asAdmin_returns200() throws Exception {
    String body = """
        {
          "title":     "Test Concert",
          "startTime": "2026-06-15T19:00:00Z",
          "venue":     "Main Arena"
        }
        """;

    mockMvc.perform(post("/api/events")
            .with(user("admin@test.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.title").value("Test Concert"))
        .andExpect(jsonPath("$.venue").value("Main Arena"))
        .andExpect(jsonPath("$.startTime").value("2026-06-15T19:00:00Z"));
  }

  @Test
  void createEvent_asUser_returns403() throws Exception {
    String body = """
        {
          "title":     "Test Concert",
          "startTime": "2026-06-15T19:00:00Z",
          "venue":     "Main Arena"
        }
        """;

    mockMvc.perform(post("/api/events")
            .with(user("user@test.com").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void createEvent_unauthenticated_returns403() throws Exception {
    String body = """
        {
          "title":     "Test Concert",
          "startTime": "2026-06-15T19:00:00Z",
          "venue":     "Main Arena"
        }
        """;

    mockMvc.perform(post("/api/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  // -------------------------------------------------------------------------
  // GET /api/events — list events (permitAll)
  // -------------------------------------------------------------------------

  @Test
  void listEvents_unauthenticated_returns200() throws Exception {
    eventService.createEvent("Event 1", Instant.parse("2026-06-01T18:00:00Z"), "Venue A");

    mockMvc.perform(get("/api/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Event 1"));
  }

  @Test
  void listEvents_pagination_works() throws Exception {
    eventService.createEvent("Event A", Instant.parse("2026-06-01T18:00:00Z"), "Venue A");
    eventService.createEvent("Event B", Instant.parse("2026-06-02T18:00:00Z"), "Venue B");
    eventService.createEvent("Event C", Instant.parse("2026-06-03T18:00:00Z"), "Venue C");

    mockMvc.perform(get("/api/events")
            .param("page", "0")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  // -------------------------------------------------------------------------
  // GET /api/events/{id} — get by ID (permitAll)
  // -------------------------------------------------------------------------

  @Test
  void getEvent_existingId_returns200() throws Exception {
    Event event = eventService.createEvent("My Event", Instant.parse("2026-07-01T20:00:00Z"), "Stadium");

    mockMvc.perform(get("/api/events/{id}", event.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(event.getId().toString()))
        .andExpect(jsonPath("$.title").value("My Event"))
        .andExpect(jsonPath("$.venue").value("Stadium"));
  }

  @Test
  void getEvent_nonExistentId_returns400() throws Exception {
    mockMvc.perform(get("/api/events/{id}", UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }
}
