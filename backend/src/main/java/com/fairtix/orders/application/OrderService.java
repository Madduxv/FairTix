package com.fairtix.orders.application;

import com.fairtix.inventory.application.SeatHoldConflictException;
import com.fairtix.inventory.domain.HoldStatus;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatHoldRepository;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.orders.domain.Order;
import com.fairtix.orders.infrastructure.OrderRepository;
import com.fairtix.tickets.application.TicketService;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import jakarta.transaction.Transactional;
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

  public OrderService(OrderRepository orderRepository,
      SeatHoldRepository seatHoldRepository,
      SeatRepository seatRepository,
      UserRepository userRepository,
      TicketService ticketService) {
    this.orderRepository = orderRepository;
    this.seatHoldRepository = seatHoldRepository;
    this.seatRepository = seatRepository;
    this.userRepository = userRepository;
    this.ticketService = ticketService;
  }

  @Transactional
  public Order createOrder(UUID userId, List<UUID> holdIds) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    // Validate all holds exist, belong to this user, and are CONFIRMED
    List<SeatHold> holds = holdIds.stream()
        .map(holdId -> seatHoldRepository.findByIdAndHolderId(holdId, userId.toString())
            .orElseThrow(() -> new IllegalArgumentException(
                "Hold not found or does not belong to you: " + holdId)))
        .toList();

    for (SeatHold hold : holds) {
      if (hold.getStatus() != HoldStatus.CONFIRMED) {
        throw new IllegalArgumentException(
            "Hold " + hold.getId() + " is not confirmed (status: " + hold.getStatus() + ")");
      }
    }

    // Transition seats from BOOKED → SOLD (guard against double-issue)
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

    // Create the order (MVP: totalAmount = 0, no payment integration)
    Order order = new Order(user, holdIds, BigDecimal.ZERO, "USD");
    order = orderRepository.save(order);

    // Issue tickets for each hold
    ticketService.issueTickets(order, holds);

    return order;
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
