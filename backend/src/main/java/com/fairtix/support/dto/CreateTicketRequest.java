package com.fairtix.support.dto;

import com.fairtix.support.domain.TicketCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to create a support ticket")
public record CreateTicketRequest(
        @NotBlank @Size(max = 200) @Schema(description = "Short description of the issue") String subject,
        @NotNull @Schema(description = "Issue category") TicketCategory category,
        @NotBlank @Schema(description = "Initial message describing the problem") String message,
        @Schema(description = "Related order ID (optional)") UUID orderId,
        @Schema(description = "Related event ID (optional)") UUID eventId) {
}
