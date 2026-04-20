package com.fairtix.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to add a message to a support ticket")
public record AddMessageRequest(
        @NotBlank @Schema(description = "Message content") String message) {
}
