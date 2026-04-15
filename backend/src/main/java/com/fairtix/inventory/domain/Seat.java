package com.fairtix.inventory.domain;

import com.fairtix.events.domain.Event;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seats",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_seat_event_section_row_number",
        columnNames = {"event_id", "section", "row_label", "seat_number"}),
    indexes = {
        @Index(name = "idx_seat_event_id", columnList = "event_id"),
        @Index(name = "idx_seat_status", columnList = "status")
    })
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @Column(nullable = false)
  private String section;

  @Column(nullable = false, name = "row_label")
  private String rowLabel;

  @Column(nullable = false, name = "seat_number")
  private String seatNumber;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SeatStatus status = SeatStatus.AVAILABLE;

  @Version
  private long version;

  public Seat(Event event, String section, String rowLabel, String seatNumber, BigDecimal price) {
    this.event = event;
    this.section = section;
    this.rowLabel = rowLabel;
    this.seatNumber = seatNumber;
    this.price = price;
    this.status = SeatStatus.AVAILABLE;
  }

  protected Seat() {
  }

  public UUID getId() {
    return id;
  }

  public Event getEvent() {
    return event;
  }

  public String getSection() {
    return section;
  }

  public String getRowLabel() {
    return rowLabel;
  }

  public String getSeatNumber() {
    return seatNumber;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public SeatStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }

  public void setStatus(SeatStatus status) {
    this.status = status;
  }
}
