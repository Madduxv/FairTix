package com.fairtix.events.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
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

  public Event(String title, String venue, Instant startTime) {
    this.title = title;
    this.venue = venue;
    this.startTime = startTime;
  }

  protected Event() {
  }

  public void update(String title, Instant startTime) {
    this.title = title;
    this.startTime = startTime;
  }

}
