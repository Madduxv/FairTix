package com.fairtix.tickets.application;

import com.fairtix.common.ResourceNotFoundException;
import com.fairtix.inventory.domain.SeatHold;
import com.fairtix.orders.domain.Order;
import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.infrastructure.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

  private final TicketRepository ticketRepository;

  public TicketService(TicketRepository ticketRepository) {
    this.ticketRepository = ticketRepository;
  }

  public void issueTickets(Order order, List<SeatHold> holds) {
    List<Ticket> tickets = holds.stream()
        .map(hold -> new Ticket(
            order,
            order.getUser(),
            hold.getSeat(),
            hold.getSeat().getEvent()))
        .toList();
    ticketRepository.saveAll(tickets);
  }

  public List<Ticket> listTickets(UUID userId) {
    return ticketRepository.findAllByUser_IdOrderByIssuedAtDesc(userId);
  }

  public Ticket getTicket(UUID ticketId, UUID userId) {
    Ticket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    if (!ticket.getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("Ticket not found: " + ticketId);
    }
    return ticket;
  }
}
