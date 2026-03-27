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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Admin-only user management endpoints.
 *
 * <p>Provides a paginated user list and the ability to promote users to admin.
 */
@Tag(name = "Admin", description = "User administration")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    /**
     * Returns a paginated list of all users.
     *
     * @param page zero-based page index
     * @param size number of users per page (max 100)
     * @return a page of user records
     */
    @Operation(summary = "List all users", description = "Admin-only. Returns a paginated list of users.")
    @ApiResponse(responseCode = "200", description = "Page of users")
    @ApiResponse(responseCode = "403", description = "Not an admin")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> listUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        return userRepository.findAll(PageRequest.of(page, Math.min(size, 100)))
                .map(UserResponse::from);
    }

    /**
     * Promotes a user to the ADMIN role.
     *
     * @param id the UUID of the user to promote
     */
    @Operation(summary = "Promote user to admin", description = "Admin-only. Sets the user's role to ADMIN.")
    @ApiResponse(responseCode = "200", description = "User promoted")
    @ApiResponse(responseCode = "400", description = "User not found")
    @PatchMapping("/users/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public void promoteUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }
}
