package com.fairtix.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Full export of the user's personal data")
public record DataExportResponse(
        @Schema(description = "User profile") Map<String, Object> profile,
        @Schema(description = "Notification preferences") Map<String, Object> notificationPreferences,
        @Schema(description = "Orders") List<Map<String, Object>> orders,
        @Schema(description = "Tickets") List<Map<String, Object>> tickets) {
}
