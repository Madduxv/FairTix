package com.fairtix.performers.dto;

import com.fairtix.performers.domain.Performer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Performer details")
public record PerformerResponse(
        @Schema(description = "Performer ID") UUID id,
        @Schema(description = "Performer name") String name,
        @Schema(description = "Music genre") String genre,
        @Schema(description = "Performer bio") String bio,
        @Schema(description = "Image URL") String imageUrl,
        @Schema(description = "Created at") Instant createdAt,
        @Schema(description = "Last updated at") Instant updatedAt) {

    public static PerformerResponse from(Performer p) {
        return new PerformerResponse(
                p.getId(), p.getName(), p.getGenre(), p.getBio(),
                p.getImageUrl(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
