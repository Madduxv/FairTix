package com.fairtix.orders.dto;

import java.util.UUID;

public record TicketSummary(UUID id, String section, String rowLabel, String seatNumber) {}
