package com.fairtix.events.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "Events", uniqueConstraints = @UniqueConstraint(columnNames = { "title", "start_time", "venue" }))
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

  @org.hibernate.validator.constraints.URL
  @Column(length = 500)
  private String thumbnail;

  public Event(String title, String venue, Instant startTime) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
    this.thumbnail = null;
  }

  public Event(String title, String venue, Instant startTime, String thumbnail) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
    this.thumbnail = thumbnail;
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

  public String getThumbnail() {
    return thumbnail;
  }

}
