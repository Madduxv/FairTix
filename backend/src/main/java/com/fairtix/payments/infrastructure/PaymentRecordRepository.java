package com.fairtix.payments.infrastructure;

import com.fairtix.payments.domain.PaymentRecord;
import com.fairtix.payments.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

  Optional<PaymentRecord> findByOrderId(UUID orderId);

  Optional<PaymentRecord> findByTransactionId(String transactionId);

  List<PaymentRecord> findByUserIdOrderByIdDesc(UUID userId);

  long countByUserIdAndStatus(UUID userId, PaymentStatus status);
}
