package com.fairtix.auth.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for login attempt lockout and password strength validation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class LoginAttemptTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // -------------------------------------------------------------------------
  // Password Strength Validation
  // -------------------------------------------------------------------------

  @ParameterizedTest(name = "{2}")
  @CsvSource({
      "weak1@test.com, Ab1!,       too short",
      "weak2@test.com, abcdefg1!,  no uppercase",
      "weak3@test.com, ABCDEFG1!,  no lowercase",
      "weak4@test.com, Abcdefgh!,  no digit",
      "weak5@test.com, Abcdefg1,   no special char",
  })
  void register_weakPassword_returns400(String email, String password, String description) throws Exception {
    String body = """
        { "email": "%s", "password": "%s" }
        """.formatted(email, password);
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
  }

  @Test
  void register_strongPassword_succeeds() throws Exception {
    String body = """
        { "email": "strong@test.com", "password": "StrongP4ss!" }
        """;
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("strong@test.com"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  // -------------------------------------------------------------------------
  // Login Attempt Lockout
  // -------------------------------------------------------------------------

  @Test
  void login_lockoutAfterMaxAttempts() throws Exception {
    // Register a user first
    String regBody = """
        { "email": "lockout@test.com", "password": "StrongP4ss!" }
        """;
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(regBody))
        .andExpect(status().isOk());

    // Fail 5 times with wrong password
    String badLogin = """
        { "email": "lockout@test.com", "password": "wrongpassword" }
        """;
    for (int i = 0; i < 5; i++) {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(badLogin))
          .andExpect(status().isUnauthorized());
    }

    // 6th attempt should be locked out (429)
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON).content(badLogin))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
        .andExpect(jsonPath("$.remainingSeconds").isNumber());
  }

  @Test
  void login_successfulLoginResetsAttempts() throws Exception {
    // Register
    String regBody = """
        { "email": "reset@test.com", "password": "StrongP4ss!" }
        """;
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON).content(regBody))
        .andExpect(status().isOk());

    // Fail 3 times
    String badLogin = """
        { "email": "reset@test.com", "password": "wrongpassword" }
        """;
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON).content(badLogin))
          .andExpect(status().isUnauthorized());
    }

    // Successful login resets
    String goodLogin = """
        { "email": "reset@test.com", "password": "StrongP4ss!" }
        """;
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON).content(goodLogin))
        .andExpect(status().isOk());

    // Can fail again without being locked
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON).content(badLogin))
        .andExpect(status().isUnauthorized());
  }
}
