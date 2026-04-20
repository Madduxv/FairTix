package com.fairtix.support.infrastructure;

import com.fairtix.support.domain.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
