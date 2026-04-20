package com.fairtix.refunds.infrastructure;

import com.fairtix.refunds.domain.RefundRequest;
import com.fairtix.refunds.domain.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<RefundRequest, UUID> {

  List<RefundRequest> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

  Optional<RefundRequest> findByOrderIdAndStatusIn(UUID orderId, List<RefundStatus> statuses);

  Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<RefundRequest> findAllByStatusOrderByCreatedAtDesc(RefundStatus status, Pageable pageable);

  List<RefundRequest> findAllByOrderId(UUID orderId);
}
