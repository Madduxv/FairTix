package com.fairtix.users.api;

import com.fairtix.users.domain.Role;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AdminController}.
 *
 * <p>All endpoints require ADMIN role.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class AdminControllerTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private MockMvc mockMvc;
  private User targetUser;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    // Create a regular user to be promoted
    targetUser = new User();
    targetUser.setEmail("target@test.com");
    targetUser.setPassword(passwordEncoder.encode("password"));
    targetUser.setRole(Role.USER);
    targetUser = userRepository.save(targetUser);
  }

  // -------------------------------------------------------------------------
  // PATCH /api/admin/users/{id}/promote
  // -------------------------------------------------------------------------

  @Test
  void promoteUser_asAdmin_returns200() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/promote", targetUser.getId())
            .with(user("admin@test.com").roles("ADMIN")))
        .andExpect(status().isOk());

    // Verify the user was actually promoted
    User updated = userRepository.findById(targetUser.getId()).orElseThrow();
    assertEquals(Role.ADMIN, updated.getRole());
  }

  @Test
  void promoteUser_asUser_returns403() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/promote", targetUser.getId())
            .with(user("user@test.com").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void promoteUser_unauthenticated_returns403() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/promote", targetUser.getId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void promoteUser_nonExistentUser_returns400() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/promote", UUID.randomUUID())
            .with(user("admin@test.com").roles("ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("User not found")));
  }
}
