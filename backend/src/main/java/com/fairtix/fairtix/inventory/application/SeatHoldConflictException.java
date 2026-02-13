package com.fairtix.fairtix.inventory.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SeatHoldConflictException extends RuntimeException {

    public SeatHoldConflictException(String message) {
        super(message);
    }
}
