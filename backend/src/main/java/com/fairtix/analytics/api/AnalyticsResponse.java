package com.fairtix.analytics.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Aggregate analytics snapshot for the admin dashboard")
public record AnalyticsResponse(
        OverviewStats overview,
        List<VenueCount> eventsByVenue,
        Map<String, Long> seatsByStatus,
        List<EventInventory> topEventsByBookings,
        Map<String, Long> holdsByStatus,
        @Schema(description = "Hold confirmation rate as a percentage (0-100)", example = "75.0")
        double holdConfirmationRate,
        List<DailyHoldCount> holdsPerDay,
        Map<String, Long> usersByRole) {

    @Schema(description = "High-level platform statistics")
    public record OverviewStats(
            @Schema(example = "42") long totalEvents,
            @Schema(example = "18") long upcomingEvents,
            @Schema(example = "350") long totalUsers,
            @Schema(example = "5000") long totalSeats,
            @Schema(example = "1200") long bookedSeats,
            @Schema(example = "85") long activeHolds) {
    }

    @Schema(description = "Event count grouped by venue")
    public record VenueCount(
            @Schema(example = "Madison Square Garden") String venue,
            @Schema(example = "12") long count) {
    }

    @Schema(description = "Seat inventory breakdown for a single event")
    public record EventInventory(
            UUID eventId,
            @Schema(example = "Summer Music Festival") String eventTitle,
            @Schema(example = "200") long available,
            @Schema(example = "50") long held,
            @Schema(example = "150") long booked) {
    }

    @Schema(description = "Hold count for a single day")
    public record DailyHoldCount(
            @Schema(example = "2026-03-25") String date,
            @Schema(example = "34") long count) {
    }
}
