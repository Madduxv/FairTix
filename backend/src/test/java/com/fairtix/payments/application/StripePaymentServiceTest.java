package com.fairtix.payments.application;

import com.fairtix.audit.application.AuditService;
import com.fairtix.payments.infrastructure.PaymentRecordRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripePaymentServiceTest {

  @Mock private PaymentRecordRepository paymentRecordRepository;
  @Mock private AuditService auditService;

  private StripePaymentService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new StripePaymentService(paymentRecordRepository, auditService);
  }

  @Test
  void verifyPaymentSucceeded_returnsTrueForSucceeded() throws Exception {
    try (MockedStatic<PaymentIntent> mocked = Mockito.mockStatic(PaymentIntent.class)) {
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      when(mockIntent.getStatus()).thenReturn("succeeded");
      mocked.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(mockIntent);

      assertTrue(service.verifyPaymentSucceeded("pi_test_123"));
    }
  }

  @Test
  void verifyPaymentSucceeded_returnsFalseForOther() throws Exception {
    try (MockedStatic<PaymentIntent> mocked = Mockito.mockStatic(PaymentIntent.class)) {
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      when(mockIntent.getStatus()).thenReturn("requires_payment_method");
      mocked.when(() -> PaymentIntent.retrieve("pi_failed")).thenReturn(mockIntent);

      assertFalse(service.verifyPaymentSucceeded("pi_failed"));
    }
  }
}
