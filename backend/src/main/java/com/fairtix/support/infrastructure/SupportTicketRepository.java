package com.fairtix.support.infrastructure;

import com.fairtix.support.domain.SupportTicket;
import com.fairtix.support.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Page<SupportTicket> findByUserId(UUID userId, Pageable pageable);

    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByUser_IdAndStatus(UUID userId, TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByUser_Id(UUID userId, Pageable pageable);

    Page<SupportTicket> findAll(Pageable pageable);
}
