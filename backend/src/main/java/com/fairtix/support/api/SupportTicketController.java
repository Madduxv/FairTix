package com.fairtix.support.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.support.application.SupportTicketService;
import com.fairtix.support.domain.TicketStatus;
import com.fairtix.support.dto.AddMessageRequest;
import com.fairtix.support.dto.AdminUpdateTicketRequest;
import com.fairtix.support.dto.CreateTicketRequest;
import com.fairtix.support.dto.SupportTicketResponse;
import com.fairtix.support.dto.TicketMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Support", description = "Support ticket management")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping("/api/support/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a support ticket")
    public SupportTicketResponse createTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request) {
        return supportTicketService.createTicket(
                principal.getUserId(),
                request.subject(),
                request.category(),
                request.message(),
                request.orderId(),
                request.eventId());
    }

    @GetMapping("/api/support/tickets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the authenticated user's support tickets")
    public Page<SupportTicketResponse> listMyTickets(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page) {
        return supportTicketService.getUserTickets(principal.getUserId(), page);
    }

    @GetMapping("/api/support/tickets/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a support ticket with its messages")
    public SupportTicketResponse getTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return supportTicketService.getTicket(id, principal.getUserId(), isAdmin);
    }

    @PostMapping("/api/support/tickets/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a message to a support ticket")
    public TicketMessageResponse addMessage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddMessageRequest request) {
        return supportTicketService.addMessage(id, principal.getUserId(), request.message());
    }

    @PostMapping("/api/support/tickets/{id}/close")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Close a support ticket")
    public SupportTicketResponse closeTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        return supportTicketService.closeTicket(id, principal.getUserId());
    }

    // ---- Admin endpoints ----

    @GetMapping("/api/admin/support/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: list all support tickets with optional status and userId filter")
    public Page<SupportTicketResponse> adminListTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page) {
        return supportTicketService.getAdminTickets(status, userId, page);
    }

    @PatchMapping("/api/admin/support/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: update ticket status, priority, or assignment")
    public SupportTicketResponse adminUpdateTicket(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminUpdateTicketRequest request) {
        return supportTicketService.adminUpdateTicket(id, principal.getUserId(), request);
    }
}
