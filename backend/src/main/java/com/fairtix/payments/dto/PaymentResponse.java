package com.fairtix.payments.dto;

import com.fairtix.orders.domain.OrderStatus;
import com.fairtix.payments.domain.PaymentRecord;
import com.fairtix.payments.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Payment processing result")
public record PaymentResponse(
    @Schema(description = "Order ID") UUID orderId,
    @Schema(description = "Order status") OrderStatus orderStatus,
    @Schema(description = "Payment status") PaymentStatus paymentStatus,
    @Schema(description = "Unique transaction ID") String transactionId,
    @Schema(description = "Amount charged") BigDecimal amount,
    @Schema(description = "Currency code") String currency,
    @Schema(description = "Failure reason if payment failed") String failureReason,
    @Schema(description = "When the payment was processed") Instant processedAt) {

  public static PaymentResponse from(PaymentRecord record, OrderStatus orderStatus) {
    return new PaymentResponse(
        record.getOrderId(),
        orderStatus,
        record.getStatus(),
        record.getTransactionId(),
        record.getAmount(),
        record.getCurrency(),
        record.getFailureReason(),
        record.getCreatedAt());
  }
}
