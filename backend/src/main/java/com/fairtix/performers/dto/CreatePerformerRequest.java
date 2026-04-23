package com.fairtix.performers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a performer")
public record CreatePerformerRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String genre,
        String bio,
        @Size(max = 500) String imageUrl) {
}
