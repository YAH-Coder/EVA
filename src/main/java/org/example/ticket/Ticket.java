package org.example.ticket;

import org.example.customer.Customer;
import org.example.customer.CustomerService;
import org.example.event.Event;
import org.example.event.EventService;

import java.time.LocalDateTime;

public class Ticket {
    private final long id;
    private final LocalDateTime purchaseDate;
    private final long customerId;
    private final long eventId;
    private final EventService eventService = EventService.getInstance();
    private final CustomerService customerService = CustomerService.getInstance();

    public Ticket(long id, LocalDateTime purchaseDate, long customerId, long eventId) {
        this.id = id;
        if (purchaseDate.isAfter(eventService.get(eventId).getDate())) {
            throw new IllegalArgumentException("Purchase date must be before event date.");
        }
        this.purchaseDate = purchaseDate;
        try {
            Customer customer = customerService.get(customerId);
            this.customerId = customerId;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Customer ID " + customerId + " not found.");
        }
        try {
            Event event = eventService.get(eventId);
            this.eventId = event.getId();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Event ID " + eventId + " not found.");
        }
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
