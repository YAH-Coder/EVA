package org.example.ticket;

import org.example.customer.Customer;
import org.example.customer.CustomerService;
import org.example.event.Event;
import org.example.event.EventService;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Ticket implements Serializable {
    private final long id;
    private final LocalDateTime purchaseDate;
    private final long customerId;
    private final long eventId;

    public Ticket(long id, LocalDateTime purchaseDate, long customerId, long eventId) {
        this.id = id;
        this.purchaseDate = purchaseDate;
        this.customerId = customerId;
        this.eventId = eventId;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public long getCustomerId() {
        return customerId;
    }


    public long getEventId() {
        return eventId;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", purchaseDate=" + purchaseDate +
                ", customerId=" + customerId +
                ", eventId=" + eventId +
                '}';
    }
}
