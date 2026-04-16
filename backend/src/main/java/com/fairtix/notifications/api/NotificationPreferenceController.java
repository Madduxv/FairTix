package com.fairtix.notifications.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.notifications.application.NotificationPreferenceService;
import com.fairtix.notifications.dto.NotificationPreferenceRequest;
import com.fairtix.notifications.dto.NotificationPreferenceResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "Notification preferences")
@RestController
@PreAuthorize("isAuthenticated()")
public class NotificationPreferenceController {

    private final NotificationPreferenceService service;

    public NotificationPreferenceController(NotificationPreferenceService service) {
        this.service = service;
    }

    @Operation(summary = "Get notification preferences")
    @ApiResponse(responseCode = "200", description = "Current preferences")
    @GetMapping("/api/users/me/notifications")
    public NotificationPreferenceResponse getPreferences(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return NotificationPreferenceResponse.from(service.getPreferences(principal.getUserId()));
    }

    @Operation(summary = "Update notification preferences")
    @ApiResponse(responseCode = "200", description = "Preferences updated")
    @PutMapping("/api/users/me/notifications")
    public NotificationPreferenceResponse updatePreferences(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody NotificationPreferenceRequest request) {
        return NotificationPreferenceResponse.from(
                service.updatePreferences(principal.getUserId(), request));
    }
}
