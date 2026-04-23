package com.fairtix.venues.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for creating a new venue")
public record CreateVenueRequest(
        @Schema(description = "Venue name", example = "Madison Square Garden")
        @NotBlank String name,
        @Schema(description = "Street address", example = "4 Pennsylvania Plaza")
        String address,
        @Schema(description = "City", example = "New York")
        String city,
        @Schema(description = "Country", example = "USA")
        String country,
        @Schema(description = "Maximum seating capacity")
        Integer capacity) {
}
