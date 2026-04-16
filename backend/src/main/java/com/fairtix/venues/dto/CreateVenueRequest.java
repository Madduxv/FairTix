package com.fairtix.venues.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requests and creates payloads for creating new venues.
 *
 * @param name the name of the venue.
 * @param address the address of the venue.
 */
public record CreateVenueRequest(
        @NotBlank @Size(max = 500) String name,
        @NotBlank @Size(max = 500) String address) {

}
