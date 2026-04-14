package com.fairtix.events.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Payload for updating an event")
public record UpdateEventRequest(
        @Schema(description = "Updated event title", example = "Summer Music Festival 2026")
        @NotBlank @Size(max = 500) String title,
        @Schema(description = "Updated start time in UTC", example = "2026-07-16T20:00:00Z")
        @NotNull Instant startTime,
        @Schema(description = "Updated thumbnail URL", example = "https://example.com/event-thumbnail.jpg")
        @URL String thumbnail) {

    public UpdateEventRequest(String title, Instant startTime) {
        this(title, startTime, null);
    }
}
