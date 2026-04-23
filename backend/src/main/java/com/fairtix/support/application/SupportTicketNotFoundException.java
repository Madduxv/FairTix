package com.fairtix.support.application;

import java.util.UUID;

public class SupportTicketNotFoundException extends RuntimeException {

    public SupportTicketNotFoundException(UUID ticketId) {
        super("Support ticket not found: " + ticketId);
    }
}
