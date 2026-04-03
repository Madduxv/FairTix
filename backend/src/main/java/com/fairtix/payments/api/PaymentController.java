package com.fairtix.payments.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.orders.application.OrderService;
import com.fairtix.orders.domain.Order;
import com.fairtix.payments.application.PaymentFailedException;
import com.fairtix.payments.dto.PaymentRequest;
import com.fairtix.payments.dto.PaymentResponse;
import com.fairtix.payments.infrastructure.PaymentRecordRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "Simulated payment processing")
@RestController
@PreAuthorize("isAuthenticated()")
public class PaymentController {

  private final OrderService orderService;
  private final PaymentRecordRepository paymentRecordRepository;

  public PaymentController(OrderService orderService,
      PaymentRecordRepository paymentRecordRepository) {
    this.orderService = orderService;
    this.paymentRecordRepository = paymentRecordRepository;
  }

  @Operation(summary = "Create order with simulated payment",
      description = "Validates holds, creates a PENDING order, runs simulated payment. "
          + "On success the order is COMPLETED and tickets are issued. "
          + "On failure/cancel the order is CANCELLED and seats are rolled back to BOOKED.")
  @ApiResponse(responseCode = "201", description = "Payment succeeded, order completed")
  @ApiResponse(responseCode = "402", description = "Payment failed or cancelled")
  @ApiResponse(responseCode = "400", description = "Invalid holds")
  @PostMapping("/api/payments/checkout")
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentResponse checkout(
      @AuthenticationPrincipal CustomUserPrincipal principal,
      @Valid @RequestBody PaymentRequest request) {

    try {
      Order order = orderService.createOrderWithPayment(
          principal.getUserId(), request.holdIds(), request.simulatedOutcome());

      var record = paymentRecordRepository.findByOrderId(order.getId())
          .orElseThrow();

      return PaymentResponse.from(record, order.getStatus());
    } catch (PaymentFailedException ex) {
      // Payment failed — return the failure details with 402
      var record = paymentRecordRepository.findByTransactionId(ex.getTransactionId())
          .orElseThrow();

      throw new PaymentProcessingException(PaymentResponse.from(record,
          com.fairtix.orders.domain.OrderStatus.CANCELLED));
    }
  }
}
