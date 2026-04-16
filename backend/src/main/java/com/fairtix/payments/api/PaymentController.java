package com.fairtix.payments.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.orders.application.OrderService;
import com.fairtix.orders.domain.Order;
import com.fairtix.payments.application.PaymentFailedException;
import com.fairtix.payments.dto.PaymentRequest;
import com.fairtix.payments.dto.PaymentResponse;
import com.fairtix.payments.infrastructure.PaymentRecordRepository;
import com.fairtix.queue.application.QueueService;
import com.fairtix.users.infrastructure.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
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
  private final UserRepository userRepository;
  private final SeatHoldRepository seatHoldRepository;
  private final QueueService queueService;

  @Value("${fairtix.payment.allow-simulated-outcome:false}")
  private boolean allowSimulatedOutcome;

  public PaymentController(OrderService orderService,
      PaymentRecordRepository paymentRecordRepository,
      UserRepository userRepository,
      SeatHoldRepository seatHoldRepository,
      QueueService queueService) {
    this.orderService = orderService;
    this.paymentRecordRepository = paymentRecordRepository;
    this.userRepository = userRepository;
    this.seatHoldRepository = seatHoldRepository;
    this.queueService = queueService;
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

    userRepository.findById(principal.getUserId()).ifPresent(user -> {
      if (!user.isEmailVerified()) {
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN,
            "Email address not verified. Please verify your email before purchasing tickets.");
      }
    });

    // For any hold belonging to a queue-required event, verify the user is admitted
    // before reaching the payment step — mirrors the gate in SeatHoldService but
    // surfaces the error earlier with a clearer message.
    request.holdIds().stream()
        .flatMap(holdId -> seatHoldRepository.findById(holdId).stream())
        .map(hold -> hold.getSeat().getEvent())
        .filter(event -> event.isQueueRequired())
        .distinct()
        .forEach(event -> {
          if (!queueService.isAdmitted(event.getId(), principal.getUserId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Queue admission required. You must be admitted from the waiting room before purchasing tickets.");
          }
        });

    if (!allowSimulatedOutcome && request.simulatedOutcome() != null) {
      throw new IllegalArgumentException(
          "simulatedOutcome is not allowed in this environment");
    }

    try {
      var outcome = allowSimulatedOutcome ? request.simulatedOutcome() : null;
      Order order = orderService.createOrderWithPayment(
          principal.getUserId(), request.holdIds(), outcome);

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
