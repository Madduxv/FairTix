package com.fairtix.performers.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.performers.domain.Performer;
import com.fairtix.performers.dto.CreatePerformerRequest;
import com.fairtix.performers.dto.UpdatePerformerRequest;
import com.fairtix.performers.infrastructure.PerformerRepository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class PerformerService {

  private final PerformerRepository repository;
  private final AuditService auditService;

  public PerformerService(PerformerRepository repository, AuditService auditService) {
    this.repository = repository;
    this.auditService = auditService;
  }

  public Performer create(CreatePerformerRequest request, UUID actorId) {
    repository.findByNameIgnoreCase(request.name()).ifPresent(p -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "A performer with that name already exists: " + p.getId());
    });
    Performer performer = new Performer(request.name(), request.genre(), request.bio(), request.imageUrl());
    Performer saved = repository.save(performer);
    auditService.log(actorId, "CREATE", "PERFORMER", saved.getId(), "Created performer: " + saved.getName());
    return saved;
  }

  public Performer get(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Performer not found: " + id));
  }

  public Page<Performer> list(Pageable pageable) {
    return repository.findAll(pageable);
  }

  public void delete(UUID id, UUID actorId) {
    Performer performer = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Performer not found: " + id));
    repository.deleteEventAssociations(id);
    repository.delete(performer);
    auditService.log(actorId, "DELETE", "PERFORMER", id, "Deleted performer: " + performer.getName());
  }

  public Performer update(UUID id, UpdatePerformerRequest request, UUID actorId) {
    Performer performer = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Performer not found: " + id));
    if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "A performer with that name already exists");
    }
    performer.update(request.name(), request.genre(), request.bio(), request.imageUrl());
    auditService.log(actorId, "UPDATE", "PERFORMER", id, "Updated performer: " + performer.getName());
    return performer;
  }
}
