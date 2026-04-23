package com.fairtix.venues.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.venues.domain.VenueSection;
import com.fairtix.venues.dto.CreateVenueSectionRequest;
import com.fairtix.venues.dto.UpdateVenueSectionRequest;
import com.fairtix.venues.infrastructure.VenueSectionRepository;
import com.fairtix.venues.infrastructure.VenueRepository;
import com.fairtix.common.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VenueSectionService {

  private final VenueSectionRepository sectionRepository;
  private final VenueRepository venueRepository;
  private final AuditService auditService;

  public VenueSectionService(VenueSectionRepository sectionRepository,
      VenueRepository venueRepository, AuditService auditService) {
    this.sectionRepository = sectionRepository;
    this.venueRepository = venueRepository;
    this.auditService = auditService;
  }

  public List<VenueSection> getSections(UUID venueId) {
    if (!venueRepository.existsById(venueId)) {
      throw new ResourceNotFoundException("Venue not found: " + venueId);
    }
    return sectionRepository.findByVenue_Id(venueId);
  }

  public VenueSection createSection(UUID venueId, CreateVenueSectionRequest request, UUID userId) {
    var venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + venueId));

    var section = new VenueSection(
        venue,
        request.name(),
        request.sectionType() != null ? request.sectionType() : "STANDARD",
        request.posX(), request.posY(),
        request.width(), request.height(),
        request.color() != null ? request.color() : "#E0E0E0",
        request.sortOrder());

    var saved = sectionRepository.save(section);
    auditService.log(userId, "CREATE", "VENUE_SECTION", saved.getId(),
        "Created section '%s' for venue %s".formatted(saved.getName(), venueId));
    return saved;
  }

  public VenueSection updateSection(UUID venueId, UUID sectionId,
      UpdateVenueSectionRequest request, UUID userId) {

    var section = sectionRepository.findById(sectionId)
        .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));

    if (!section.getVenue().getId().equals(venueId)) {
      throw new IllegalArgumentException(
          "Section %s does not belong to venue %s".formatted(sectionId, venueId));
    }

    section.update(
        request.name(),
        request.sectionType() != null ? request.sectionType() : "STANDARD",
        request.posX(), request.posY(),
        request.width(), request.height(),
        request.color() != null ? request.color() : "#E0E0E0",
        request.sortOrder());

    var saved = sectionRepository.save(section);
    auditService.log(userId, "UPDATE", "VENUE_SECTION", saved.getId(),
        "Updated section '%s' for venue %s".formatted(saved.getName(), venueId));
    return saved;
  }

  public void deleteSection(UUID venueId, UUID sectionId, UUID userId) {
    var section = sectionRepository.findById(sectionId)
        .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));

    if (!section.getVenue().getId().equals(venueId)) {
      throw new IllegalArgumentException(
          "Section %s does not belong to venue %s".formatted(sectionId, venueId));
    }

    sectionRepository.delete(section);
    auditService.log(userId, "DELETE", "VENUE_SECTION", sectionId,
        "Deleted section from venue %s".formatted(venueId));
  }
}
