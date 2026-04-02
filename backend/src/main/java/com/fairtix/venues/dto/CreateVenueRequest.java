package com.fairtix.venues.dto;
import jakarta.validation.constraints.NotBlank;

/**
 * Requests and creates payloads for creating new venues.
 *
 * @param name the name of the venue.
 * @param address the address of the venue.
 */
public record CreateVenueRequest(
        @NotBlank String name,
        @NotBlank String address) {

}
