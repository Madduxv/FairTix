package com.fairtix.venues.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVenueSectionRequest(
        @NotBlank String name,
        String sectionType,
        double posX,
        double posY,
        double width,
        double height,
        String color,
        int sortOrder) {
}
