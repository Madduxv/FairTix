package com.fairtix.tickets.infrastructure;

import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  List<Ticket> findAllByUser_IdOrderByIssuedAtDesc(UUID userId);

  List<Ticket> findAllByOrder_Id(UUID orderId);

  long countByUser_IdAndEvent_IdAndStatusNot(UUID userId, UUID eventId, TicketStatus status);

  long countByUser_IdAndEvent_IdAndStatusNotIn(UUID userId, UUID eventId, java.util.Collection<TicketStatus> statuses);

  /** Cancel cascade: find all tickets for a given event with a given status. */
  List<Ticket> findAllByEvent_IdAndStatus(UUID eventId, TicketStatus status);
}
