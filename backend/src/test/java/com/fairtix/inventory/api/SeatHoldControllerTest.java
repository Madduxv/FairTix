package com.fairtix.inventory.api;

import com.fairtix.auth.WithMockPrincipal;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SeatHoldController} focused on HTTP-layer
 * concerns:
 * request validation, error response shape, and status codes.
 *
 * <p>
 * Uses a full Spring context (H2) so GlobalExceptionHandler and bean
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
  private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

  @BeforeEach
  void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // -------------------------------------------------------------------------
  // Input validation → 400 VALIDATION_ERROR
  // -------------------------------------------------------------------------

  @Test
  void createHold_emptySeatIds_returns400WithValidationError() throws Exception {
    String body = """
        {
          "seatIds":  []
        }
        """;

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
        .with(WithMockPrincipal.admin(TEST_USER_ID, "admin@test.com"))
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
    String body = """
        {}
        """;

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
        .with(WithMockPrincipal.admin(TEST_USER_ID, "admin@test.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void createHold_durationZero_returns400WithValidationError() throws Exception {
    String body = """
        {
          "seatIds":         ["%s"],
          "durationMinutes": 0
        }
        """.formatted(UUID.randomUUID());

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
        .with(WithMockPrincipal.admin(TEST_USER_ID, "admin@test.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("durationMinutes")));
  }

  @Test
  void createHold_nonExistentEvent_returns400WithBadRequest() throws Exception {
    String body = """
        {
          "seatIds":  ["%s"]
        }
        """.formatted(UUID.randomUUID());

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
        .with(WithMockPrincipal.admin(TEST_USER_ID, "admin@test.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value(containsString("Event not found")));
  }

  @Test
  void errorResponse_alwaysContainsRequiredFields() throws Exception {
    String body = """
        {
          "seatIds":  []
        }
        """;

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
        .with(WithMockPrincipal.admin(TEST_USER_ID, "admin@test.com"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.path").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  // -------------------------------------------------------------------------
  // Security → 403 when unauthenticated (anonymous user)
  // -------------------------------------------------------------------------

  @Test
  void createHold_unauthenticated_returns401() throws Exception {
    String body = """
        {
          "seatIds":  ["%s"]
        }
        """.formatted(UUID.randomUUID());

    mockMvc.perform(post(CREATE_URL, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isUnauthorized());
  }
}
