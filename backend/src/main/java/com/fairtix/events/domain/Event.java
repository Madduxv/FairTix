package com.fairtix.events.domain;

import java.time.Instant;
import java.util.UUID;

import com.fairtix.venues.domain.Venue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "Events", uniqueConstraints = @UniqueConstraint(columnNames = { "title", "start_time", "venue_id" }))
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

  @org.hibernate.validator.constraints.URL
  @Column(length = 500)
  private String thumbnail;

  @Column(name = "organizer_id")
  private UUID organizerId;

  @Column(name = "queue_required", nullable = false)
  private boolean queueRequired = false;

  @Column(name = "queue_capacity")
  private Integer queueCapacity;

  @Column(name = "max_tickets_per_user")
  private Integer maxTicketsPerUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventStatus status = EventStatus.DRAFT;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Column(name = "cancellation_reason")
  private String cancellationReason;

  @Version
  private long version;

  public Event(String title, Venue venue, Instant startTime, UUID organizerId) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
    this.thumbnail = null;
    this.organizerId = organizerId;
  }

  public Event(String title, Venue venue, Instant startTime, String thumbnail, UUID organizerId) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
    this.thumbnail = thumbnail;
    this.organizerId = organizerId;
    this.status = EventStatus.DRAFT;
  }

  protected Event() {
  }

  public void update(String title, Instant startTime) {
    this.title = title;
    this.startTime = startTime;
  }

  public void update(String title, Instant startTime, String thumbnail) {
    this.title = title;
    this.startTime = startTime;
    this.thumbnail = thumbnail;
  }
  public void updateQueueSettings(boolean queueRequired, Integer queueCapacity) {
    this.queueRequired = queueRequired;
    this.queueCapacity = queueCapacity;
  }

  // --- Lifecycle transitions ---

  public void publish() {
    if (this.status != EventStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT events can be published (current: " + this.status + ")");
    }
    this.status = EventStatus.PUBLISHED;
    this.publishedAt = Instant.now();
  }

  public void activate() {
    if (this.status != EventStatus.PUBLISHED) {
      throw new IllegalStateException("Only PUBLISHED events can be activated (current: " + this.status + ")");
    }
    this.status = EventStatus.ACTIVE;
  }

  public void complete() {
    if (this.status != EventStatus.ACTIVE) {
      throw new IllegalStateException("Only ACTIVE events can be completed (current: " + this.status + ")");
    }
    this.status = EventStatus.COMPLETED;
    this.completedAt = Instant.now();
  }

  public void cancel(String reason) {
    if (this.status == EventStatus.COMPLETED || this.status == EventStatus.CANCELLED || this.status == EventStatus.ARCHIVED) {
      throw new IllegalStateException("Cannot cancel an event in status: " + this.status);
    }
    this.status = EventStatus.CANCELLED;
    this.cancelledAt = Instant.now();
    this.cancellationReason = reason;
  }

  public void archive() {
    if (this.status != EventStatus.COMPLETED && this.status != EventStatus.CANCELLED) {
      throw new IllegalStateException("Only COMPLETED or CANCELLED events can be archived (current: " + this.status + ")");
    }
    this.status = EventStatus.ARCHIVED;
    this.archivedAt = Instant.now();
  }

  // --- Getters ---
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

  public String getThumbnail() {
    return thumbnail;
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

  public EventStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public long getVersion() {
    return version;
  }
}
