package com.fairtix.events.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Events")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false)
  private String venue;

  @Column(nullable = false)
  private Instant startTime;

  @Column(name = "organizer_id")
  private UUID organizerId;

  @Column(name = "queue_required", nullable = false)
  private boolean queueRequired = false;

  @Column(name = "queue_capacity")
  private Integer queueCapacity;

  public Event(String title, String venue, Instant startTime, UUID organizerId) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
    this.organizerId = organizerId;
  }

  protected Event() {
  }

  public void update(String title, Instant startTime) {
    this.title = title;
    this.startTime = startTime;
  }

  public void updateQueueSettings(boolean queueRequired, Integer queueCapacity) {
    this.queueRequired = queueRequired;
    this.queueCapacity = queueCapacity;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getVenue() {
    return venue;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public UUID getOrganizerId() {
    return organizerId;
  }

  public boolean isQueueRequired() {
    return queueRequired;
  }

  public Integer getQueueCapacity() {
    return queueCapacity;
  }
}
