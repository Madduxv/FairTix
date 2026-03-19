package com.fairtix.auth.api;

import com.fairtix.auth.application.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 *
 * <p>All /auth/** endpoints are permitAll so no mock user is needed.
 * Tests verify registration, login, duplicate-email handling, and JWT validity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class AuthControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JwtService jwtService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // -------------------------------------------------------------------------
  // POST /auth/register
  // -------------------------------------------------------------------------

  @Test
  void register_success_returnsToken() throws Exception {
    String body = """
        {
          "email":    "newuser@test.com",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(notNullValue()));
  }

  @Test
  void register_duplicateEmail_returns409() throws Exception {
    String body = """
        {
          "email":    "duplicate@test.com",
          "password": "password123"
        }
        """;

    // First registration succeeds
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());

    // Second registration with same email fails
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void register_tokenIsValidJwt() throws Exception {
    String body = """
        {
          "email":    "jwtcheck@test.com",
          "password": "password123"
        }
        """;

    MvcResult result = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andReturn();

    String token = com.jayway.jsonpath.JsonPath
        .read(result.getResponse().getContentAsString(), "$.token");

    // Token should be parseable and contain the registered email
    assertDoesNotThrow(() -> jwtService.extractAllClaims(token));
    String email = jwtService.extractEmail(token);
    assertEquals("jwtcheck@test.com", email);
  }

  // -------------------------------------------------------------------------
  // POST /auth/login
  // -------------------------------------------------------------------------

  @Test
  void login_success_returnsToken() throws Exception {
    // Register first
    String registerBody = """
        {
          "email":    "loginuser@test.com",
          "password": "password123"
        }
        """;
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody))
        .andExpect(status().isOk());

    // Login
    String loginBody = """
        {
          "email":    "loginuser@test.com",
          "password": "password123"
        }
        """;
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(notNullValue()));
  }

  @Test
  void login_badPassword_returns401() throws Exception {
    // Register first
    String registerBody = """
        {
          "email":    "badpw@test.com",
          "password": "password123"
        }
        """;
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody))
        .andExpect(status().isOk());

    // Login with wrong password
    String loginBody = """
        {
          "email":    "badpw@test.com",
          "password": "wrongpassword"
        }
        """;
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_nonExistentUser_returns401() throws Exception {
    String body = """
        {
          "email":    "nobody@test.com",
          "password": "password123"
        }
        """;

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isUnauthorized());
  }
}
