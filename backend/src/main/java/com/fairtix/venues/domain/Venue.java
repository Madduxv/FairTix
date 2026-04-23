package com.fairtix.venues.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "venues")
public class Venue {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, unique = true, length = 255)
  private String name;

  @Column(length = 500)
  private String address;

  @Column(length = 100)
  private String city;

  @Column(length = 100)
  private String country;

  private Integer capacity;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Venue(String name, String address, String city, String country, Integer capacity) {
    this.name = name;
    this.address = address;
    this.city = city;
    this.country = country;
    this.capacity = capacity;
  }

  protected Venue() {
  }

  public void update(String name, String address, String city, String country, Integer capacity) {
    this.name = name;
    this.address = address;
    this.city = city;
    this.country = country;
    this.capacity = capacity;
    this.updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getAddress() { return address; }
  public String getCity() { return city; }
  public String getCountry() { return country; }
  public Integer getCapacity() { return capacity; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
