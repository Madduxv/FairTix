package com.fairtix.refunds.dto;

import jakarta.validation.constraints.Size;

public record RefundApprovalRequest(
    boolean approved,
    @Size(max = 1000) String adminNotes
) {}
