package com.fairtix.venues.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.events.infrastructure.EventRepository;
import com.fairtix.venues.domain.Venue;
import com.fairtix.venues.dto.CreateVenueRequest;
import com.fairtix.venues.dto.UpdateVenueRequest;
import com.fairtix.venues.infrastructure.VenueRepository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class VenueService {

  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final AuditService auditService;

  public VenueService(VenueRepository venueRepository,
                      EventRepository eventRepository,
                      AuditService auditService) {
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.auditService = auditService;
  }

  public Venue createVenue(CreateVenueRequest request, UUID actorId) {
    if (venueRepository.findByName(request.name()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists: " + request.name());
    }
    Venue venue = new Venue(request.name(), request.address(), request.city(), request.country(), request.capacity(),
        request.latitude(), request.longitude());
    venue = venueRepository.save(venue);
    auditService.log(actorId, "CREATE", "VENUE", venue.getId(), "Created venue: " + venue.getName());
    return venue;
  }

  public Venue getVenue(UUID id) {
    return venueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
  }

  public Page<Venue> listVenues(Pageable pageable) {
    return venueRepository.findAll(pageable);
  }

  public Venue updateVenue(UUID id, UpdateVenueRequest request, UUID actorId) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
    if (venueRepository.existsByNameAndIdNot(request.name(), id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists: " + request.name());
    }
    venue.update(request.name(), request.address(), request.city(), request.country(), request.capacity(),
        request.latitude(), request.longitude());
    auditService.log(actorId, "UPDATE", "VENUE", id, "Updated venue: " + venue.getName());
    return venue;
  }

  public void deleteVenue(UUID id, UUID actorId) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + id));
    if (eventRepository.existsByVenue_Id(id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Cannot delete venue with associated events");
    }
    venueRepository.delete(venue);
    auditService.log(actorId, "DELETE", "VENUE", id, "Deleted venue: " + venue.getName());
  }
}
