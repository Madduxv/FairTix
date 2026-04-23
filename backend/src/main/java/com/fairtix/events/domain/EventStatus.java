package com.fairtix.events.domain;

public enum EventStatus {
    DRAFT,       // Created but not visible to the public
    PUBLISHED,   // Visible, tickets not yet on sale
    ACTIVE,      // On sale — seats can be held and purchased
    COMPLETED,   // Event has occurred, no new purchases
    CANCELLED,   // Cancelled — triggers hold release and ticket cancellation cascade
    ARCHIVED     // Hidden from all views, retained for records
}
