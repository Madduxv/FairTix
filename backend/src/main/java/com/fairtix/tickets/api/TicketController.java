package com.fairtix.tickets.api;

import com.fairtix.tickets.application.TicketService;
import com.fairtix.tickets.dto.TicketResponse;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tickets", description = "Ticket retrieval")
@RestController
@PreAuthorize("isAuthenticated()")
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService, UserRepository userRepository) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "List the current user's tickets")
    @ApiResponse(responseCode = "200", description = "List of tickets")
    @GetMapping("/api/tickets")
    public List<TicketResponse> listTickets(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = resolveUserId(principal);
        return ticketService.listTickets(userId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Operation(summary = "Get ticket details")
    @ApiResponse(responseCode = "200", description = "Ticket found")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    @GetMapping("/api/tickets/{ticketId}")
    public TicketResponse getTicket(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID ticketId) {
        UUID userId = resolveUserId(principal);
        return TicketResponse.from(ticketService.getTicket(ticketId, userId));
    }

    private UUID resolveUserId(UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        return user.getId();
    }
}
