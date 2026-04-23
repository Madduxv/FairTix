package com.fairtix.orders.infrastructure;

import com.fairtix.orders.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  List<Order> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

  @Query(value = "SELECT CAST(created_at AS DATE), SUM(total_amount) FROM orders WHERE status = 'COMPLETED' AND created_at >= :since GROUP BY CAST(created_at AS DATE) ORDER BY 1", nativeQuery = true)
  List<Object[]> revenuePerDay(@Param("since") Instant since);

  @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'COMPLETED'", nativeQuery = true)
  BigDecimal totalRevenue();
}
