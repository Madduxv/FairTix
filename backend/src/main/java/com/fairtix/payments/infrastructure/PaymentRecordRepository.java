package com.fairtix.payments.infrastructure;

import com.fairtix.payments.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

  Optional<PaymentRecord> findByOrderId(UUID orderId);

  Optional<PaymentRecord> findByTransactionId(String transactionId);
}
