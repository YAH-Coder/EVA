package org.example;

import org.example.customer.CustomerService;
import org.example.customer.CustomerServiceInterface;
import org.example.event.EventService;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketService;
import org.example.ticket.TicketServiceInterface;
// import org.example.utils.IDServiceParallel; // Removed

/**
 * Represents the main entry point for interacting with the ticket shop services.
 * This class provides access to customer, event, and ticket management functionalities.
 */
public class TicketShop {
    private final CustomerServiceInterface customerServiceInterface;
    private final EventServiceInterface eventServiceInterface;
    private final TicketServiceInterface ticketServiceInterface;

    /**
     * Constructs a new TicketShop instance.
     * Initializes the customer, event, and ticket services.
     *
     * @throws InterruptedException if the initialization of services is interrupted.
     */
    public TicketShop() throws InterruptedException { // Changed constructor signature
        this.customerServiceInterface = CustomerService.getInstance();
        this.eventServiceInterface = EventService.getInstance();
        this.ticketServiceInterface = TicketService.getInstance();
    }

    /**
     * Gets the customer service interface.
     *
     * @return The customer service interface.
     */
    public CustomerServiceInterface getCustomerServiceInterface() {
        return customerServiceInterface;
    }

    /**
     * Gets the event service interface.
     *
     * @return The event service interface.
     */
    public EventServiceInterface getEventServiceInterface() {
        return eventServiceInterface;
    }

    /**
     * Gets the ticket service interface.
     *
     * @return The ticket service interface.
     */
    public TicketServiceInterface getTicketServiceInterface() {
        return ticketServiceInterface;
    }
}
