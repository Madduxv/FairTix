package com.fairtix.queue.application;

public class QueueConflictException extends RuntimeException {

    public QueueConflictException(String message) {
        super(message);
    }
}
