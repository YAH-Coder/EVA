package org.example.ticket;

import org.example.customer.Customer;
import org.example.customer.CustomerService;
import org.example.event.Event;
import org.example.event.EventService;

import java.time.LocalDateTime;

/**
 * Represents a ticket purchased by a customer for a specific event.
 * Each ticket has a unique ID, a purchase date, and references to the customer who bought it and the event it's for.
 */
public class Ticket {
    private final long id;
    private final LocalDateTime purchaseDate;
    private final long customerId;
    private final long eventId;
    private final EventService eventService = EventService.getInstance();
    private final CustomerService customerService = CustomerService.getInstance();

    /**
     * Constructs a new Ticket instance.
     * Validates that the purchase date is before the event date, and that the customer and event exist.
     *
     * @param id The unique ID of the ticket.
     * @param purchaseDate The date and time of the ticket purchase. Must be before the event date.
     * @param customerId The ID of the customer purchasing the ticket. Must correspond to an existing customer.
     * @param eventId The ID of the event for which the ticket is purchased. Must correspond to an existing event.
     * @throws InterruptedException If service calls are interrupted (though less likely for get operations).
     * @throws IllegalArgumentException If the purchase date is after the event date, or if the customer or event ID is not found.
     */
    public Ticket(long id, LocalDateTime purchaseDate, long customerId, long eventId) throws InterruptedException {
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

    /**
     * Gets the unique ID of the ticket.
     *
     * @return The ticket ID.
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the purchase date of the ticket.
     *
     * @return The purchase date.
     */
    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * Gets the ID of the customer who purchased the ticket.
     *
     * @return The customer ID.
     */
    public long getCustomerId() {
        return customerId;
    }

    /**
     * Gets the ID of the event for which the ticket was purchased.
     *
     * @return The event ID.
     */
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
