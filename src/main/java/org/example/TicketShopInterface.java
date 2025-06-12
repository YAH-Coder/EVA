package org.example;

import org.example.customer.CustomerServiceInterface;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;

public interface TicketShopInterface {
    CustomerServiceInterface getCustomerServiceInterface();

    EventServiceInterface getEventServiceInterface();

    TicketServiceInterface getTicketServiceInterface();
}
