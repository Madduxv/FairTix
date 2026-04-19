package com.fairtix.payments.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.payments.domain.PaymentRecord;
import com.fairtix.payments.domain.PaymentStatus;
import com.fairtix.payments.infrastructure.PaymentRecordRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StripePaymentService {

  @Value("${stripe.secret-key:}")
  private String secretKey;

  private final PaymentRecordRepository paymentRecordRepository;
  private final AuditService auditService;

  public StripePaymentService(PaymentRecordRepository paymentRecordRepository,
      AuditService auditService) {
    this.paymentRecordRepository = paymentRecordRepository;
    this.auditService = auditService;
  }

  public String createPaymentIntent(long amountCents, String currency) {
    Stripe.apiKey = secretKey;
    try {
      PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
          .setAmount(amountCents)
          .setCurrency(currency)
          .build();
      PaymentIntent intent = PaymentIntent.create(params);
      return intent.getClientSecret();
    } catch (StripeException e) {
      throw new RuntimeException("Failed to create Stripe payment intent: " + e.getMessage(), e);
    }
  }

  public boolean verifyPaymentSucceeded(String paymentIntentId, long expectedAmountCents) {
    Stripe.apiKey = secretKey;
    try {
      PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
      return "succeeded".equals(intent.getStatus())
          && intent.getAmountReceived() == expectedAmountCents;
    } catch (StripeException e) {
      throw new RuntimeException("Failed to verify Stripe payment: " + e.getMessage(), e);
    }
  }

  public PaymentRecord recordStripePayment(String paymentIntentId, UUID orderId, UUID userId,
      BigDecimal amount, String currency) {
    PaymentRecord record = new PaymentRecord(
        orderId, userId, amount, currency, PaymentStatus.SUCCESS, paymentIntentId, null);
    PaymentRecord saved = paymentRecordRepository.save(record);
    auditService.log(userId, "PAYMENT_PROCESSED", "PAYMENT", saved.getId(),
        "stripe_intent=" + paymentIntentId);
    return saved;
  }
}
