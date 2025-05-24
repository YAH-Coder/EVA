package org.example;

import org.example.customer.CustomerService;
import org.example.customer.CustomerServiceInterface;
import org.example.event.EventService;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketService;
import org.example.ticket.TicketServiceInterface;
// import org.example.utils.IDServiceParallel; // Removed

public class TicketShop {
    private final CustomerServiceInterface customerServiceInterface;
    private final EventServiceInterface eventServiceInterface;
    private final TicketServiceInterface ticketServiceInterface;

    public TicketShop() throws InterruptedException { // Changed constructor signature
        this.customerServiceInterface = CustomerService.getInstance();
        this.eventServiceInterface = EventService.getInstance();
        this.ticketServiceInterface = TicketService.getInstance();
    }

    public CustomerServiceInterface getCustomerServiceInterface() {
        return customerServiceInterface;
    }

    public EventServiceInterface getEventServiceInterface() {
        return eventServiceInterface;
    }

    public TicketServiceInterface getTicketServiceInterface() {
        return ticketServiceInterface;
    }
}
