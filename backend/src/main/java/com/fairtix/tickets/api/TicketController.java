package com.fairtix.tickets.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.tickets.application.TicketService;
import com.fairtix.tickets.dto.TicketResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tickets", description = "Ticket retrieval")
@RestController
@PreAuthorize("isAuthenticated()")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "List the current user's tickets")
    @ApiResponse(responseCode = "200", description = "List of tickets")
    @GetMapping("/api/tickets")
    public List<TicketResponse> listTickets(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ticketService.listTickets(principal.getUserId()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Operation(summary = "Get ticket details")
    @ApiResponse(responseCode = "200", description = "Ticket found")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    @GetMapping("/api/tickets/{ticketId}")
    public TicketResponse getTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID ticketId) {
        return TicketResponse.from(ticketService.getTicket(ticketId, principal.getUserId()));
    }
}
