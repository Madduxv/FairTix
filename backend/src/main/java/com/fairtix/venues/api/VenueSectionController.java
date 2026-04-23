package com.fairtix.venues.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.venues.application.VenueSectionService;
import com.fairtix.venues.dto.CreateVenueSectionRequest;
import com.fairtix.venues.dto.UpdateVenueSectionRequest;
import com.fairtix.venues.dto.VenueSectionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Venue Sections", description = "Section layout definitions for venues")
@RestController
@RequestMapping("/api/venues/{venueId}/sections")
public class VenueSectionController {

    private final VenueSectionService sectionService;

    public VenueSectionController(VenueSectionService sectionService) {
        this.sectionService = sectionService;
    }

    @Operation(summary = "List sections for a venue", description = "Public.")
    @ApiResponse(responseCode = "200", description = "Sections listed")
    @SecurityRequirements
    @PermitAll
    @GetMapping
    public List<VenueSectionResponse> getSections(@PathVariable UUID venueId) {
        return sectionService.getSections(venueId).stream()
                .map(s -> VenueSectionResponse.from(s, venueId))
                .toList();
    }

    @Operation(summary = "Create a section", description = "Admin-only.")
    @ApiResponse(responseCode = "201", description = "Section created")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueSectionResponse createSection(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateVenueSectionRequest request) {
        var section = sectionService.createSection(venueId, request, principal.getUserId());
        return VenueSectionResponse.from(section, venueId);
    }

    @Operation(summary = "Update a section", description = "Admin-only.")
    @ApiResponse(responseCode = "200", description = "Section updated")
    @ApiResponse(responseCode = "400", description = "Section not found or wrong venue")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{sectionId}")
    public VenueSectionResponse updateSection(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID venueId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateVenueSectionRequest request) {
        var section = sectionService.updateSection(venueId, sectionId, request, principal.getUserId());
        return VenueSectionResponse.from(section, venueId);
    }

    @Operation(summary = "Delete a section", description = "Admin-only.")
    @ApiResponse(responseCode = "204", description = "Section deleted")
    @ApiResponse(responseCode = "400", description = "Section not found or wrong venue")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID venueId,
            @PathVariable UUID sectionId) {
        sectionService.deleteSection(venueId, sectionId, principal.getUserId());
    }
}
