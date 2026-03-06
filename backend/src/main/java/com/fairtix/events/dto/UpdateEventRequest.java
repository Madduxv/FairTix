package com.fairtix.events.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
    @NotNull @Size(max = 500) String title,
    @NotNull Instant startTime) {
}
