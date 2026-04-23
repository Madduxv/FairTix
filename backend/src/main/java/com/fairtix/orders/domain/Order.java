package com.fairtix.orders.domain;

import com.fairtix.users.domain.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status")
})
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false, precision = 10, scale = 2, name = "total_amount")
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 3)
  private String currency;

  @ElementCollection
  @CollectionTable(name = "order_holds", joinColumns = @JoinColumn(name = "order_id"))
  @Column(name = "hold_id")
  private List<UUID> holdIds = new ArrayList<>();

  @Column(nullable = false, name = "created_at", updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  public Order(User user, List<UUID> holdIds, BigDecimal totalAmount, String currency) {
    this(user, holdIds, totalAmount, currency, OrderStatus.COMPLETED);
  }

  public Order(User user, List<UUID> holdIds, BigDecimal totalAmount, String currency,
      OrderStatus status) {
    this.user = user;
    this.holdIds = new ArrayList<>(holdIds);
    this.totalAmount = totalAmount;
    this.currency = currency;
    this.status = status;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  protected Order() {
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getCurrency() {
    return currency;
  }

  public List<UUID> getHoldIds() {
    return holdIds;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }
}
