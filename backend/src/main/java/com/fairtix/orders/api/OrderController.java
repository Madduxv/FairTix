package com.fairtix.orders.api;

import com.fairtix.orders.application.OrderService;
import com.fairtix.orders.dto.CreateOrderRequest;
import com.fairtix.orders.dto.OrderResponse;
import com.fairtix.users.domain.User;
import com.fairtix.users.infrastructure.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders", description = "Order creation and retrieval")
@RestController
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Create an order from confirmed holds")
    @ApiResponse(responseCode = "201", description = "Order created with tickets issued")
    @ApiResponse(responseCode = "400", description = "Invalid holds")
    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateOrderRequest request) {
        UUID userId = resolveUserId(principal);
        return OrderResponse.from(orderService.createOrder(userId, request.holdIds()));
    }

    @Operation(summary = "List the current user's orders")
    @ApiResponse(responseCode = "200", description = "List of orders")
    @GetMapping("/api/orders")
    public List<OrderResponse> listOrders(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = resolveUserId(principal);
        return orderService.listOrders(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Operation(summary = "Get order details")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/api/orders/{orderId}")
    public OrderResponse getOrder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID orderId) {
        UUID userId = resolveUserId(principal);
        return OrderResponse.from(orderService.getOrder(orderId, userId));
    }

    private UUID resolveUserId(UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getId();
    }
}
