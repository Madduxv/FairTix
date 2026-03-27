package com.fairtix.tickets.infrastructure;

import com.fairtix.tickets.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  List<Ticket> findAllByUser_IdOrderByIssuedAtDesc(UUID userId);

  List<Ticket> findAllByOrder_Id(UUID orderId);
}
