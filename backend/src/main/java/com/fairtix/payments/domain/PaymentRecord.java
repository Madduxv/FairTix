package com.fairtix.payments.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_records", indexes = {
    @Index(name = "idx_payment_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_user_id", columnList = "user_id")
})
public class PaymentRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, name = "order_id")
  private UUID orderId;

  @Column(nullable = false, name = "user_id")
  private UUID userId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(name = "transaction_id", unique = true, nullable = false)
  private String transactionId;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(nullable = false, name = "created_at", updatable = false)
  private Instant createdAt;

  protected PaymentRecord() {
  }

  public PaymentRecord(UUID orderId, UUID userId, BigDecimal amount, String currency,
      PaymentStatus status, String transactionId, String failureReason) {
    this.orderId = orderId;
    this.userId = userId;
    this.amount = amount;
    this.currency = currency;
    this.status = status;
    this.transactionId = transactionId;
    this.failureReason = failureReason;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public UUID getUserId() {
    return userId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
