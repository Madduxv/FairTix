package com.fairtix.users.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairtix.users.infrastructure.UserRepository;
import com.fairtix.users.domain.User;
import com.fairtix.users.domain.Role;
import com.fairtix.users.dto.UserResponse;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final UserRepository userRepository;

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public Page<UserResponse> listUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return userRepository.findAll(PageRequest.of(page, Math.min(size, 100)))
        .map(UserResponse::from);
  }

  @PatchMapping("/users/{id}/promote")
  @PreAuthorize("hasRole('ADMIN')")
  public void promoteUser(@PathVariable UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

    user.setRole(Role.ADMIN);
    userRepository.save(user);
  }
}
