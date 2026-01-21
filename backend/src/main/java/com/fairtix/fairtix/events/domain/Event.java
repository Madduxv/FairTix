package com.fairtix.fairtix.events.domain;

import java.time.Instant;
import java.time.LocalDateTime;
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

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String venue;

  @Column(nullable = false)
  private Instant startTime;

  public Event(String title, String venue, Instant startTime) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
  }

  protected Event() {
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

}
