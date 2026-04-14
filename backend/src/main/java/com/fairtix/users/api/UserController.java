package com.fairtix.users.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.users.application.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "User account management")
@RestController
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Delete own account",
      description = "Soft-deletes the authenticated user's account. "
          + "Releases all active and confirmed holds. Anonymizes user data. "
          + "Existing orders and tickets are preserved for record-keeping.")
  @ApiResponse(responseCode = "204", description = "Account deleted")
  @ApiResponse(responseCode = "404", description = "User not found")
  @DeleteMapping("/api/users/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteOwnAccount(@AuthenticationPrincipal CustomUserPrincipal principal) {
    userService.deleteAccount(principal.getUserId());
  }
}
