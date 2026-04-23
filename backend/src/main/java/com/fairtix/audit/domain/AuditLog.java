package com.fairtix.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 50)
  private String action;

  @Column(nullable = false, length = 50)
  private String resourceType;

  private UUID resourceId;

  @Column(columnDefinition = "TEXT")
  private String details;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected AuditLog() {
  }

  public AuditLog(UUID userId, String action, String resourceType, UUID resourceId, String details) {
    this.userId = userId;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.details = details;
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public String getAction() { return action; }
  public String getResourceType() { return resourceType; }
  public UUID getResourceId() { return resourceId; }
  public String getDetails() { return details; }
  public Instant getCreatedAt() { return createdAt; }
}
