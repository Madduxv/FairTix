package com.fairtix.inventory.dto;

import java.util.List;

public record ImportResultResponse(
    int imported,
    int skipped,
    List<RowError> errors
) {
    public record RowError(int row, String message) {}
}
