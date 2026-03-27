package com.fairtix.orders.infrastructure;

import com.fairtix.orders.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  List<Order> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
}
