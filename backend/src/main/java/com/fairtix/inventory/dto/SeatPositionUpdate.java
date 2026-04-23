package com.fairtix.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SeatPositionUpdate(
        @NotNull UUID id,
        @NotNull Double posX,
        @NotNull Double posY,
        Double rotation) {
}
