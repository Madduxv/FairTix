package com.fairtix.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelEventRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason) {
}
