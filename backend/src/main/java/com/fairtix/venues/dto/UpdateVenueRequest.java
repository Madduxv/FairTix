package com.fairtix.venues.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for updating a venue")
public record UpdateVenueRequest(
        @Schema(description = "Venue name", example = "Madison Square Garden")
        @NotBlank String name,
        @Schema(description = "Street address")
        String address,
        @Schema(description = "City")
        String city,
        @Schema(description = "Country")
        String country,
        @Schema(description = "Maximum seating capacity")
        Integer capacity) {
}
