package com.fairtix.inventory.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SeatHoldController} focused on HTTP-layer concerns:
 * request validation, error response shape, and status codes.
 *
 * <p>Uses a full Spring context (H2) so GlobalExceptionHandler and bean
 * validation are both active. MockMvc is built manually from the
 * WebApplicationContext to avoid any autoconfigure package dependencies
 * that may vary between Spring Boot versions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class SeatHoldControllerTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  private static final String CREATE_URL = "/api/events/{eventId}/holds";

  @BeforeEach
  void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  // -------------------------------------------------------------------------
  // Input validation → 400 VALIDATION_ERROR
  // -------------------------------------------------------------------------

  @Test
  void createHold_emptySeatIds_returns400WithValidationError() throws Exception {
    String body = """
        {
          "seatIds":  [],
          "holderId": "user-1"
        }
        """;

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value(containsString("seatIds")));
  }

  @Test
  void createHold_missingSeatIds_returns400WithValidationError() throws Exception {
    // seatIds field omitted entirely — null treated as empty by @NotEmpty
    String body = """
        {
          "holderId": "user-1"
        }
        """;

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void createHold_blankHolderId_returns400WithValidationError() throws Exception {
    String body = """
        {
          "seatIds":  ["%s"],
          "holderId": "   "
        }
        """.formatted(UUID.randomUUID());

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("holderId")));
  }

  @Test
  void createHold_durationZero_returns400WithValidationError() throws Exception {
    String body = """
        {
          "seatIds":         ["%s"],
          "holderId":        "user-1",
          "durationMinutes": 0
        }
        """.formatted(UUID.randomUUID());

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("durationMinutes")));
  }

  @Test
  void createHold_nonExistentEvent_returns400WithBadRequest() throws Exception {
    // Valid body but event doesn't exist → IllegalArgumentException → 400 BAD_REQUEST
    String body = """
        {
          "seatIds":  ["%s"],
          "holderId": "user-1"
        }
        """.formatted(UUID.randomUUID());

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }

  @Test
  void errorResponse_alwaysContainsRequiredFields() throws Exception {
    // Any GlobalExceptionHandler path must emit status/code/message/path/timestamp
    String body = """
        {
          "seatIds":  [],
          "holderId": "user-1"
        }
        """;

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
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
