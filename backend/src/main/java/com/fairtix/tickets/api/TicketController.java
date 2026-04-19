package com.fairtix.tickets.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.events.domain.Event;
import com.fairtix.inventory.domain.Seat;
import com.fairtix.tickets.application.TicketService;
import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.dto.TicketResponse;
import com.fairtix.venues.domain.Venue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Tag(name = "Tickets", description = "Ticket retrieval")
@RestController
@PreAuthorize("isAuthenticated()")
public class TicketController {

    private static final DateTimeFormatter ICS_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

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

    @Operation(summary = "Download ticket as iCal (.ics)")
    @ApiResponse(responseCode = "200", description = "ICS file")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    @GetMapping("/api/tickets/{ticketId}/calendar.ics")
    public ResponseEntity<String> getCalendar(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID ticketId) {
        Ticket ticket = ticketService.getTicketForCalendar(ticketId, principal.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fairtix-event.ics\"")
                .body(buildIcs(ticket));
    }

    private String buildIcs(Ticket ticket) {
        Event event = ticket.getEvent();
        Seat seat = ticket.getSeat();
        Venue venue = event.getVenue();

        String location = venue != null
                ? escapeIcs(Stream.of(venue.getName(), venue.getAddress(), venue.getCity())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(", ")))
                : "";
        String description = escapeIcs(
                "Ticket ID: " + ticket.getId() + ". Seat: "
                + seat.getSection() + " " + seat.getRowLabel() + " " + seat.getSeatNumber());

        return "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//FairTix//FairTix//EN\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + ticket.getId() + "@fairtix.com\r\n"
                + "DTSTART:" + ICS_DATE_FORMAT.format(event.getStartTime()) + "\r\n"
                + "SUMMARY:" + escapeIcs(event.getTitle()) + "\r\n"
                + "LOCATION:" + location + "\r\n"
                + "DESCRIPTION:" + description + "\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";
    }

    private String escapeIcs(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
