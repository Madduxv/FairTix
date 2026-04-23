package com.fairtix.venues.dto;

import com.fairtix.venues.domain.VenueSection;

import java.util.UUID;

public record VenueSectionResponse(
        UUID id,
        UUID venueId,
        String name,
        String sectionType,
        String pathData,
        double posX,
        double posY,
        double width,
        double height,
        String color,
        int sortOrder) {

    public static VenueSectionResponse from(VenueSection section, UUID venueId) {
        return new VenueSectionResponse(
                section.getId(),
                venueId,
                section.getName(),
                section.getSectionType(),
                section.getPathData(),
                section.getPosX(),
                section.getPosY(),
                section.getWidth(),
                section.getHeight(),
                section.getColor(),
                section.getSortOrder());
    }
}
