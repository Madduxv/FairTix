package com.fairtix.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification preference update request")
public record NotificationPreferenceRequest(
        @Schema(description = "Email on order completion") boolean emailOrder,
        @Schema(description = "Email on ticket issuance") boolean emailTicket,
        @Schema(description = "Email on hold creation/expiry") boolean emailHold,
        @Schema(description = "Marketing emails (opt-in only)") boolean emailMarketing) {
}
