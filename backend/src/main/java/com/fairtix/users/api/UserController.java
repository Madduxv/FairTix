package com.fairtix.users.api;

import com.fairtix.auth.domain.CustomUserPrincipal;
import com.fairtix.notifications.application.NotificationPreferenceService;
import com.fairtix.notifications.domain.NotificationPreference;
import com.fairtix.orders.domain.Order;
import com.fairtix.orders.infrastructure.OrderRepository;
import com.fairtix.tickets.domain.Ticket;
import com.fairtix.tickets.infrastructure.TicketRepository;
import com.fairtix.users.application.UserService;
import com.fairtix.users.domain.User;
import com.fairtix.users.dto.DataExportResponse;
import com.fairtix.users.infrastructure.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Tag(name = "Users", description = "User account management")
@RestController
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserService userService;
  private final UserRepository userRepository;
  private final OrderRepository orderRepository;
  private final TicketRepository ticketRepository;
  private final NotificationPreferenceService notificationPreferenceService;

  public UserController(UserService userService,
      UserRepository userRepository,
      OrderRepository orderRepository,
      TicketRepository ticketRepository,
      NotificationPreferenceService notificationPreferenceService) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.orderRepository = orderRepository;
    this.ticketRepository = ticketRepository;
    this.notificationPreferenceService = notificationPreferenceService;
  }

  @Operation(summary = "Delete own account",
      description = "Soft-deletes the authenticated user's account. "
          + "Releases all active and confirmed holds. Anonymizes user data. "
          + "Existing orders and tickets are preserved for record-keeping.")
  @ApiResponse(responseCode = "204", description = "Account deleted")
  @ApiResponse(responseCode = "404", description = "User not found")
  @DeleteMapping("/api/users/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteOwnAccount(@AuthenticationPrincipal CustomUserPrincipal principal) {
    userService.deleteAccount(principal.getUserId());
  }

  @Operation(summary = "Export personal data",
      description = "Returns a JSON dump of all personal data for the authenticated user, "
          + "including profile, notification preferences, orders, and tickets.")
  @ApiResponse(responseCode = "200", description = "Data export")
  @GetMapping("/api/users/me/data-export")
  public DataExportResponse exportData(@AuthenticationPrincipal CustomUserPrincipal principal) {
    UUID userId = principal.getUserId();

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    // Profile
    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put("userId", user.getId());
    profile.put("email", user.getEmail());
    profile.put("role", user.getRole().name());

    // Notification preferences
    NotificationPreference prefs = notificationPreferenceService.getPreferences(userId);
    Map<String, Object> notifPrefs = new LinkedHashMap<>();
    notifPrefs.put("emailOrder", prefs.isEmailOrder());
    notifPrefs.put("emailTicket", prefs.isEmailTicket());
    notifPrefs.put("emailHold", prefs.isEmailHold());
    notifPrefs.put("emailMarketing", prefs.isEmailMarketing());

    // Orders
    List<Map<String, Object>> orders = orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
        .stream()
        .map(this::orderToMap)
        .toList();

    // Tickets
    List<Map<String, Object>> tickets = ticketRepository.findAllByUser_IdOrderByIssuedAtDesc(userId)
        .stream()
        .map(this::ticketToMap)
        .toList();

    return new DataExportResponse(profile, notifPrefs, orders, tickets);
  }

  private Map<String, Object> orderToMap(Order order) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("orderId", order.getId());
    map.put("status", order.getStatus().name());
    map.put("totalAmount", order.getTotalAmount());
    map.put("currency", order.getCurrency());
    map.put("createdAt", order.getCreatedAt());
    return map;
  }

  private Map<String, Object> ticketToMap(Ticket ticket) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("ticketId", ticket.getId());
    map.put("eventTitle", ticket.getEvent().getTitle());
    map.put("eventVenue", ticket.getEvent().getVenue() != null ? ticket.getEvent().getVenue().getName() : null);
    map.put("eventStartTime", ticket.getEvent().getStartTime());
    map.put("seatSection", ticket.getSeat().getSection());
    map.put("seatRow", ticket.getSeat().getRowLabel());
    map.put("seatNumber", ticket.getSeat().getSeatNumber());
    map.put("price", ticket.getPrice());
    map.put("status", ticket.getStatus().name());
    map.put("issuedAt", ticket.getIssuedAt());
    return map;
  }
}
