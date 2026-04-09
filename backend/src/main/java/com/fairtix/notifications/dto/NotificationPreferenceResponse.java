package com.fairtix.notifications.dto;

import com.fairtix.notifications.domain.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification preference settings")
public record NotificationPreferenceResponse(
        boolean emailOrder,
        boolean emailTicket,
        boolean emailHold,
        boolean emailMarketing) {

    public static NotificationPreferenceResponse from(NotificationPreference prefs) {
        return new NotificationPreferenceResponse(
                prefs.isEmailOrder(),
                prefs.isEmailTicket(),
                prefs.isEmailHold(),
                prefs.isEmailMarketing());
    }
}
