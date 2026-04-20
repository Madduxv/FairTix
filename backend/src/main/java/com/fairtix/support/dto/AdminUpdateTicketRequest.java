package com.fairtix.support.dto;

import com.fairtix.support.domain.TicketPriority;
import com.fairtix.support.domain.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Admin request to update a support ticket's status, priority, or assignment")
public record AdminUpdateTicketRequest(
        @Schema(description = "New status (optional)") TicketStatus status,
        @Schema(description = "New priority (optional)") TicketPriority priority,
        @Schema(description = "Assign to user ID (optional)") UUID assignedTo) {
}
