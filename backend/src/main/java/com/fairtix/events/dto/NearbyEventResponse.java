package com.fairtix.events.dto;

import com.fairtix.events.domain.Event;
import com.fairtix.venues.dto.VenueResponse;
import com.fairtix.performers.dto.PerformerResponse;
import com.fairtix.events.domain.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Event with distance from a geographic search origin")
public record NearbyEventResponse(
        UUID id,
        String title,
        Instant startTime,
        VenueResponse venue,
        UUID organizerId,
        boolean queueRequired,
        Integer queueCapacity,
        Integer maxTicketsPerUser,
        EventStatus status,
        Instant publishedAt,
        Instant cancelledAt,
        Instant completedAt,
        Instant archivedAt,
        String cancellationReason,
        List<PerformerResponse> performers,
        @Schema(description = "Distance from search origin in kilometres", example = "12.4")
        double distanceKm) {

    public static NearbyEventResponse from(Event event, double distanceKm) {
        return new NearbyEventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartTime(),
                event.getVenue() != null ? VenueResponse.from(event.getVenue()) : null,
                event.getOrganizerId(),
                event.isQueueRequired(),
                event.getQueueCapacity(),
                event.getMaxTicketsPerUser(),
                event.getStatus(),
                event.getPublishedAt(),
                event.getCancelledAt(),
                event.getCompletedAt(),
                event.getArchivedAt(),
                event.getCancellationReason(),
                event.getPerformers().stream().map(PerformerResponse::from).collect(Collectors.toList()),
                distanceKm);
    }
}
