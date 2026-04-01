package com.fairtix.orders.application;

import com.fairtix.inventory.application.SeatHoldConflictException;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.orders.domain.Order;
import com.fairtix.orders.domain.OrderStatus;
import com.fairtix.orders.infrastructure.OrderRepository;
import com.fairtix.payments.application.PaymentFailedException;
import com.fairtix.payments.application.PaymentSimulationService;
import com.fairtix.payments.domain.PaymentRecord;
import com.fairtix.payments.domain.PaymentStatus;
import com.fairtix.tickets.application.TicketService;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final SeatHoldRepository seatHoldRepository;
  private final SeatRepository seatRepository;
  private final UserRepository userRepository;
  private final TicketService ticketService;
  private final PaymentSimulationService paymentSimulationService;

  /** Simulated price per seat until a real pricing model is added. */
  private static final BigDecimal SIMULATED_SEAT_PRICE = new BigDecimal("25.00");

  public OrderService(OrderRepository orderRepository,
      SeatHoldRepository seatHoldRepository,
      SeatRepository seatRepository,
      UserRepository userRepository,
      TicketService ticketService,
      PaymentSimulationService paymentSimulationService) {
    this.orderRepository = orderRepository;
    this.seatHoldRepository = seatHoldRepository;
    this.seatRepository = seatRepository;
    this.userRepository = userRepository;
    this.ticketService = ticketService;
    this.paymentSimulationService = paymentSimulationService;
  }

  /**
   * Original order creation (MVP path, no payment). Always succeeds with $0 total.
   */
  @Transactional
  public Order createOrder(UUID userId, List<UUID> holdIds) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    List<SeatHold> holds = validateHolds(userId, holdIds);

    for (SeatHold hold : holds) {
      Seat seat = hold.getSeat();
      if (seat.getStatus() == SeatStatus.SOLD) {
        throw new SeatHoldConflictException(
            "Seat " + seat.getId() + " has already been sold");
      }
      if (seat.getStatus() != SeatStatus.BOOKED) {
        throw new SeatHoldConflictException(
            "Seat " + seat.getId() + " is not in BOOKED state (status: " + seat.getStatus() + ")");
      }
      seat.setStatus(SeatStatus.SOLD);
      seatRepository.save(seat);
    }

    Order order = new Order(user, holdIds, BigDecimal.ZERO, "USD");
    order = orderRepository.save(order);
    ticketService.issueTickets(order, holds);
    return order;
  }

  /**
   * Creates an order with simulated payment processing.
   *
   * Flow:
   * 1. Validate holds belong to user and are CONFIRMED
   * 2. Transition seats BOOKED → SOLD
   * 3. Create order as PENDING
   * 4. Process simulated payment
   * 5. On success: mark order COMPLETED, issue tickets
   * 6. On failure/cancel: mark order CANCELLED, rollback seats to BOOKED
   */
  @Transactional(noRollbackFor = PaymentFailedException.class)
  public Order createOrderWithPayment(UUID userId, List<UUID> holdIds,
      PaymentStatus simulatedOutcome) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    List<SeatHold> holds = validateHolds(userId, holdIds);

    // Transition seats from BOOKED → SOLD
    for (SeatHold hold : holds) {
      Seat seat = hold.getSeat();
      if (seat.getStatus() == SeatStatus.SOLD) {
        throw new SeatHoldConflictException(
            "Seat " + seat.getId() + " has already been sold");
      }
      if (seat.getStatus() != SeatStatus.BOOKED) {
        throw new SeatHoldConflictException(
            "Seat " + seat.getId() + " is not in BOOKED state (status: " + seat.getStatus() + ")");
      }
      seat.setStatus(SeatStatus.SOLD);
      seatRepository.save(seat);
    }

    // Calculate total (simulated flat price per seat)
    BigDecimal totalAmount = SIMULATED_SEAT_PRICE.multiply(BigDecimal.valueOf(holds.size()));

    // Create order as PENDING
    Order order = new Order(user, holdIds, totalAmount, "USD", OrderStatus.PENDING);
    order = orderRepository.save(order);

    // Process simulated payment
    PaymentRecord payment = paymentSimulationService.processPayment(
        order.getId(), userId, totalAmount, "USD", simulatedOutcome);

    if (payment.getStatus() == PaymentStatus.SUCCESS) {
      order.setStatus(OrderStatus.COMPLETED);
      orderRepository.save(order);
      ticketService.issueTickets(order, holds);
    } else {
      // Rollback: mark order cancelled and revert seats to BOOKED
      order.setStatus(OrderStatus.CANCELLED);
      orderRepository.save(order);
      for (SeatHold hold : holds) {
        Seat seat = hold.getSeat();
        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);
      }
      throw new PaymentFailedException(
          payment.getFailureReason(), payment.getStatus(), payment.getTransactionId());
    }

    return order;
  }

  private List<SeatHold> validateHolds(UUID userId, List<UUID> holdIds) {
    List<SeatHold> holds = holdIds.stream()
        .map(holdId -> seatHoldRepository.findByIdAndOwnerId(holdId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Hold not found or does not belong to you: " + holdId)))
        .toList();

    for (SeatHold hold : holds) {
      if (hold.getStatus() != HoldStatus.CONFIRMED) {
        throw new IllegalArgumentException(
            "Hold " + hold.getId() + " is not confirmed (status: " + hold.getStatus() + ")");
      }
    }
    return holds;
  }

  public Order getOrder(UUID orderId, UUID userId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    if (!order.getUser().getId().equals(userId)) {
      throw new OrderNotFoundException("Order not found: " + orderId);
    }
    return order;
  }

  public List<Order> listOrders(UUID userId) {
    return orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
  }
}
