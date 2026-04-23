package com.fairtix.venues.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venue_sections",
    indexes = @Index(name = "idx_venue_sections_venue", columnList = "venue_id"))
public class VenueSection {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "section_type", nullable = false, length = 30)
  private String sectionType = "STANDARD";

  @Column(name = "path_data", columnDefinition = "TEXT")
  private String pathData;

  @Column(name = "pos_x", nullable = false)
  private double posX = 0;

  @Column(name = "pos_y", nullable = false)
  private double posY = 0;

  @Column(nullable = false)
  private double width = 100;

  @Column(nullable = false)
  private double height = 100;

  @Column(length = 7)
  private String color = "#E0E0E0";

  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected VenueSection() {
  }

  public VenueSection(Venue venue, String name, String sectionType, double posX, double posY,
      double width, double height, String color, int sortOrder) {
    this.venue = venue;
    this.name = name;
    this.sectionType = sectionType;
    this.posX = posX;
    this.posY = posY;
    this.width = width;
    this.height = height;
    this.color = color;
    this.sortOrder = sortOrder;
  }

  public UUID getId() { return id; }
  public Venue getVenue() { return venue; }
  public String getName() { return name; }
  public String getSectionType() { return sectionType; }
  public String getPathData() { return pathData; }
  public double getPosX() { return posX; }
  public double getPosY() { return posY; }
  public double getWidth() { return width; }
  public double getHeight() { return height; }
  public String getColor() { return color; }
  public int getSortOrder() { return sortOrder; }
  public Instant getCreatedAt() { return createdAt; }

  public void update(String name, String sectionType, double posX, double posY,
      double width, double height, String color, int sortOrder) {
    this.name = name;
    this.sectionType = sectionType;
    this.posX = posX;
    this.posY = posY;
    this.width = width;
    this.height = height;
    this.color = color;
    this.sortOrder = sortOrder;
  }
}
