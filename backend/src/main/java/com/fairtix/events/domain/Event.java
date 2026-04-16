package com.fairtix.events.domain;

import java.time.Instant;
import java.util.UUID;

import com.fairtix.venues.domain.Venue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Events")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, length = 500)
  private String title;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id")
  private Venue venue;

  @Column(nullable = false)
  private Instant startTime;

  @Column(name = "organizer_id")
  private UUID organizerId;

  @Column(name = "queue_required", nullable = false)
  private boolean queueRequired = false;

  @Column(name = "queue_capacity")
  private Integer queueCapacity;

  @Column(name = "max_tickets_per_user")
  private Integer maxTicketsPerUser;

  public Event(String title, Venue venue, Instant startTime, UUID organizerId) {
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

  public Venue getVenue() {
    return venue;
  }

  public void setVenue(Venue venue) {
    this.venue = venue;
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

  public Integer getMaxTicketsPerUser() {
    return maxTicketsPerUser;
  }

  public void setMaxTicketsPerUser(Integer maxTicketsPerUser) {
    this.maxTicketsPerUser = maxTicketsPerUser;
  }
}
