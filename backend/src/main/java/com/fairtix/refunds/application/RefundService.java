package com.fairtix.refunds.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.fraud.application.RiskScoringService;
import com.fairtix.fraud.domain.RiskTier;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.inventory.domain.SeatStatus;
import com.fairtix.inventory.infrastructure.SeatRepository;
import com.fairtix.notifications.application.EmailService;
import com.fairtix.notifications.application.EmailTemplateService;
import com.fairtix.orders.domain.Order;
import com.fairtix.orders.domain.OrderStatus;
import com.fairtix.orders.infrastructure.OrderRepository;
import com.fairtix.payments.domain.PaymentRecord;
import com.fairtix.payments.domain.PaymentStatus;
import com.fairtix.payments.infrastructure.PaymentRecordRepository;
import com.fairtix.refunds.domain.RefundRequest;
import com.fairtix.refunds.domain.RefundStatus;
import com.fairtix.refunds.infrastructure.RefundRepository;
import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.domain.TicketStatus;
import com.fairtix.tickets.infrastructure.TicketRepository;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

  private static final Logger log = LoggerFactory.getLogger(RefundService.class);

  private final RefundRepository refundRepository;
  private final OrderRepository orderRepository;
  private final TicketRepository ticketRepository;
  private final SeatRepository seatRepository;
  private final PaymentRecordRepository paymentRecordRepository;
  private final UserRepository userRepository;
  private final AuditService auditService;
  private final EmailService emailService;
  private final EmailTemplateService emailTemplateService;
  private final RiskScoringService riskScoringService;

  @Value("${fairtix.refund.enabled:true}")
  private boolean refundEnabled;

  @Value("${fairtix.refund.time-window-days:14}")
  private int refundWindowDays;

  @Value("${fairtix.refund.auto-approve-threshold:50.00}")
  private BigDecimal autoApproveThreshold;

  public RefundService(RefundRepository refundRepository,
      OrderRepository orderRepository,
      TicketRepository ticketRepository,
      SeatRepository seatRepository,
      PaymentRecordRepository paymentRecordRepository,
      UserRepository userRepository,
      AuditService auditService,
      EmailService emailService,
      EmailTemplateService emailTemplateService,
      RiskScoringService riskScoringService) {
    this.refundRepository = refundRepository;
    this.orderRepository = orderRepository;
    this.ticketRepository = ticketRepository;
    this.seatRepository = seatRepository;
    this.paymentRecordRepository = paymentRecordRepository;
    this.userRepository = userRepository;
    this.auditService = auditService;
    this.emailService = emailService;
    this.emailTemplateService = emailTemplateService;
    this.riskScoringService = riskScoringService;
  }

  @Transactional
  public RefundRequest requestRefund(UUID userId, UUID orderId, String reason) {
    if (!refundEnabled) {
      throw new RefundNotEligibleException("Refunds are not currently enabled.");
    }

    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

    if (!order.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("Order not found: " + orderId);
    }

    if (order.getStatus() != OrderStatus.COMPLETED) {
      throw new RefundNotEligibleException(
          "Refunds can only be requested for completed orders. Order status: " + order.getStatus());
    }

    // Check no pending refund exists for this order
    refundRepository.findByOrderIdAndStatusIn(orderId,
        List.of(RefundStatus.REQUESTED, RefundStatus.PENDING_MANUAL, RefundStatus.APPROVED))
        .ifPresent(existing -> {
          throw new RefundNotEligibleException(
              "A refund request is already pending for this order (status: " + existing.getStatus() + ")");
        });

    // Validate no tickets are USED
    List<Ticket> tickets = ticketRepository.findAllByOrder_Id(orderId);
    boolean anyUsed = tickets.stream().anyMatch(t -> t.getStatus() == TicketStatus.USED);
    if (anyUsed) {
      throw new RefundNotEligibleException("Cannot refund an order with used tickets.");
    }

    // Validate refund time window
    Instant cutoff = Instant.now().minus(refundWindowDays, ChronoUnit.DAYS);
    if (order.getCreatedAt().isBefore(cutoff)) {
      throw new RefundNotEligibleException(
          "Refund window of " + refundWindowDays + " days has expired.");
    }

    BigDecimal amount = order.getTotalAmount();
    RefundRequest refund = new RefundRequest(orderId, userId, amount, reason);
    refund = refundRepository.save(refund);

    auditService.log(userId, "REFUND_REQUESTED", "REFUND", refund.getId(),
        "Refund requested for order " + orderId + ", amount=" + amount);

    RiskTier tier = riskScoringService.getTier(userId);
    if (tier == RiskTier.HIGH || tier == RiskTier.CRITICAL) {
      refund.holdForManualReview();
      refundRepository.save(refund);
      auditService.log(userId, "REFUND_HELD_FRAUD_RISK", "REFUND", refund.getId(),
          "tier=" + tier);
      return refund;
    }

    // Auto-approve if amount is below the configured threshold
    if (amount.compareTo(autoApproveThreshold) <= 0) {
      refund.approve(userId, "Auto-approved: amount within threshold");
      refundRepository.save(refund);
      auditService.log(userId, "REFUND_AUTO_APPROVED", "REFUND", refund.getId(),
          "Auto-approved refund for order " + orderId + ", amount=" + amount);
      processRefund(refund, userId);
    } else {
      sendRefundRequestedEmail(userId, refund);
    }
    return refund;
  }

  @Transactional
  public RefundRequest reviewRefund(UUID adminUserId, UUID refundId, boolean approved, String notes) {
    RefundRequest refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + refundId));

    if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.PENDING_MANUAL) {
      throw new IllegalStateException(
          "Refund is not in REQUESTED or PENDING_MANUAL status (current: " + refund.getStatus() + ")");
    }

    if (approved) {
      refund.approve(adminUserId, notes);
      refundRepository.save(refund);
      auditService.log(adminUserId, "REFUND_APPROVED", "REFUND", refundId,
          "Refund approved for order " + refund.getOrderId());
      processRefund(refund, adminUserId);
    } else {
      refund.reject(adminUserId, notes);
      refundRepository.save(refund);
      auditService.log(adminUserId, "REFUND_REJECTED", "REFUND", refundId,
          "Refund rejected for order " + refund.getOrderId() + ": " + notes);
      sendRefundRejectedEmail(refund, notes);
    }

    return refund;
  }

  @Transactional
  public void processRefund(RefundRequest refund, UUID actorId) {
    Order order = orderRepository.findById(refund.getOrderId())
        .orElseThrow(() -> new IllegalStateException("Order not found during refund processing"));

    order.setStatus(OrderStatus.REFUNDED);
    orderRepository.save(order);

    // Create a negative-amount PaymentRecord for audit trail
    paymentRecordRepository.findByOrderId(refund.getOrderId()).ifPresent(original -> {
      PaymentRecord refundRecord = new PaymentRecord(
          refund.getOrderId(), refund.getUserId(),
          refund.getAmount().negate(), order.getCurrency(),
          PaymentStatus.REFUNDED,
          "REFUND-" + refund.getId().toString().substring(0, 8).toUpperCase(),
          null);
      paymentRecordRepository.save(refundRecord);
    });

    // Mark tickets as REFUNDED and release seats
    List<Ticket> tickets = ticketRepository.findAllByOrder_Id(refund.getOrderId());
    for (Ticket ticket : tickets) {
      ticket.setStatus(TicketStatus.REFUNDED);
      ticketRepository.save(ticket);
      Seat seat = ticket.getSeat();
      seat.setStatus(SeatStatus.AVAILABLE);
      seatRepository.save(seat);
    }

    refund.complete();
    refundRepository.save(refund);

    auditService.log(actorId, "REFUND_COMPLETED", "REFUND", refund.getId(),
        "Refund completed for order " + refund.getOrderId() + ", amount=" + refund.getAmount());

    sendRefundCompletedEmail(refund);
  }

  /**
   * Called by EventService when an event is cancelled.
   * Auto-approves and processes refunds for all COMPLETED orders on this event.
   */
  @Transactional
  public void processCancellationRefunds(UUID eventId, UUID actorId) {
    List<Ticket> tickets = ticketRepository.findAllByEvent_IdAndStatus(eventId, TicketStatus.VALID);
    // Group by order and create/process refunds
    tickets.stream()
        .map(t -> t.getOrder())
        .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
        .distinct()
        .forEach(order -> {
          // Skip only if this order has already been definitively refunded
          boolean alreadyRefunded = refundRepository.findAllByOrderId(order.getId()).stream()
              .anyMatch(r -> r.getStatus() == RefundStatus.COMPLETED);
          if (alreadyRefunded) return;

          try {
            RefundRequest refund = new RefundRequest(
                order.getId(), order.getUser().getId(),
                order.getTotalAmount(), "Event cancelled");
            refund.approve(actorId, "Automatic refund due to event cancellation");
            refund = refundRepository.save(refund);
            processRefund(refund, actorId);
          } catch (Exception ex) {
            log.error("Failed to auto-process cancellation refund for order {}: {}",
                order.getId(), ex.getMessage());
          }
        });
  }

  public List<RefundRequest> getUserRefunds(UUID userId) {
    return refundRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
  }

  public RefundRequest getRefund(UUID refundId, UUID userId) {
    RefundRequest refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundId));
    if (!refund.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Refund not found: " + refundId);
    }
    return refund;
  }

  public Page<RefundRequest> adminListRefunds(RefundStatus status, Pageable pageable) {
    if (status != null) {
      return refundRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
    }
    return refundRepository.findAllByOrderByCreatedAtDesc(pageable);
  }

  // -------------------------------------------------------------------------

  private void sendRefundRequestedEmail(UUID userId, RefundRequest refund) {
    try {
      User user = userRepository.findById(userId).orElse(null);
      if (user == null) return;
      String body = emailTemplateService.buildRefundRequestedEmail(
          user.getEmail(), refund.getOrderId().toString(), refund.getAmount().toPlainString(), refund.getReason());
      emailService.sendEmail(user.getEmail(), "Your FairTix refund request was received", body);
    } catch (Exception ex) {
      log.warn("Failed to send refund-requested email for refund {}: {}", refund.getId(), ex.getMessage());
    }
  }

  private void sendRefundCompletedEmail(RefundRequest refund) {
    try {
      User user = userRepository.findById(refund.getUserId()).orElse(null);
      if (user == null) return;
      String body = emailTemplateService.buildRefundCompletedEmail(
          user.getEmail(), refund.getOrderId().toString(), refund.getAmount().toPlainString());
      emailService.sendEmail(user.getEmail(), "Your FairTix refund has been processed", body);
    } catch (Exception ex) {
      log.warn("Failed to send refund-completed email for refund {}: {}", refund.getId(), ex.getMessage());
    }
  }

  private void sendRefundRejectedEmail(RefundRequest refund, String adminNotes) {
    try {
      User user = userRepository.findById(refund.getUserId()).orElse(null);
      if (user == null) return;
      String body = emailTemplateService.buildRefundRejectedEmail(
          user.getEmail(), refund.getOrderId().toString(), refund.getReason(), adminNotes);
      emailService.sendEmail(user.getEmail(), "Update on your FairTix refund request", body);
    } catch (Exception ex) {
      log.warn("Failed to send refund-rejected email for refund {}: {}", refund.getId(), ex.getMessage());
    }
  }
}
