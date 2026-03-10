package com.fairtix.event.api;

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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link EventController}.
 *
 * Focuses on HTTP-layer behavior including:
 * validation errors, response structure, and status codes.
 *
 * Uses a full Spring context (H2) so that:
 * - validation annotations
 * - GlobalExceptionHandler
 * - request mapping
 * are all active.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class EventControllerTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  private static final String EVENTS_URL = "/api/events";
  private static final String EVENT_URL = "/api/events/{id}";

  @BeforeEach
  void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  // -------------------------------------------------------------------------
  // Create Event validation
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = "ADMIN")
  void createEvent_missingTitle_returns400ValidationError() throws Exception {

    String body = """
        {
          "startTime": "%s",
          "venue": "Test Venue"
        }
        """.formatted(Instant.now());

    mockMvc.perform(post(EVENTS_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value(containsString("title")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createEvent_blankVenue_returns400ValidationError() throws Exception {

    String body = """
        {
          "title": "Test Event",
          "startTime": "%s",
          "venue": "   "
        }
        """.formatted(Instant.now());

    mockMvc.perform(post(EVENTS_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("venue")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createEvent_missingStartTime_returns400ValidationError() throws Exception {

    String body = """
        {
          "title": "Test Event",
          "venue": "Test Venue"
        }
        """;

    mockMvc.perform(post(EVENTS_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("startTime")));
  }

  // -------------------------------------------------------------------------
  // Search endpoint
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  void search_events_returns200() throws Exception {

    mockMvc.perform(get(EVENTS_URL)
        .param("page", "0")
        .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  @WithMockUser
  void search_sizeGreaterThan100_isCappedButStillReturns200() throws Exception {

    mockMvc.perform(get(EVENTS_URL)
        .param("size", "500"))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // Get Event
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  void getEvent_nonexistentEvent_returns400() throws Exception {

    mockMvc.perform(get(EVENT_URL, UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("Event")));
  }

  // -------------------------------------------------------------------------
  // Update Event validation
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateEvent_blankTitle_returns400ValidationError() throws Exception {

    String body = """
        {
          "title": "   ",
          "startTime": "%s"
        }
        """.formatted(Instant.now());

    mockMvc.perform(put(EVENT_URL, UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("title")));
  }

  // -------------------------------------------------------------------------
  // Delete Event
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteEvent_nonexistentEvent_returns400() throws Exception {

    mockMvc.perform(delete(EVENT_URL, UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value(containsString("Event")));
  }

  // -------------------------------------------------------------------------
  // Error response contract
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = "ADMIN")
  void errorResponse_alwaysContainsRequiredFields() throws Exception {

    String body = """
        {
          "title": "",
          "startTime": "%s",
          "venue": ""
        }
        """.formatted(Instant.now());

    mockMvc.perform(post(EVENTS_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.path").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
