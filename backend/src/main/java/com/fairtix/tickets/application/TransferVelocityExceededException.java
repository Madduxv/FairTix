package com.fairtix.tickets.application;

public class TransferVelocityExceededException extends RuntimeException {
    public TransferVelocityExceededException(int max, int windowHours) {
        super("Transfer limit exceeded: maximum " + max + " transfers per " + windowHours + " hours");
    }
}
