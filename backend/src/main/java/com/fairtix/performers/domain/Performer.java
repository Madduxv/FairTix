package com.fairtix.performers.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "performers")
public class Performer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 100)
  private String genre;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Performer(String name, String genre, String bio, String imageUrl) {
    this.name = name;
    this.genre = genre;
    this.bio = bio;
    this.imageUrl = imageUrl;
  }

  protected Performer() {
  }

  public void update(String name, String genre, String bio, String imageUrl) {
    this.name = name;
    this.genre = genre;
    this.bio = bio;
    this.imageUrl = imageUrl;
    this.updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getGenre() { return genre; }
  public String getBio() { return bio; }
  public String getImageUrl() { return imageUrl; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
