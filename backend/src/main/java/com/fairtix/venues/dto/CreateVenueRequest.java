package com.fairtix.venues.dto;
import java.time.Instant;

public record CreateVenueRequest(String name, String address, String event, Instant startTime) {

}
