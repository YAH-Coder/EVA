package org.example.spring;

import org.example.TicketShopInterface;
import org.example.client.CLIClient;
import org.example.customer.CustomerServiceInterface;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;

public class TicketShopRest implements TicketShopInterface {
    EventServiceInterface eventServiceInterface;
    CustomerServiceInterface customerServiceInterface;
    TicketServiceInterface ticketServiceInterface;

    public TicketShopRest() {
        eventServiceInterface = new EventServiceRest();
    }

    @Override
    public CustomerServiceInterface getCustomerServiceInterface() {
        return null;
    }

    @Override
    public EventServiceInterface getEventServiceInterface() {
        return eventServiceInterface;
    }

    @Override
    public TicketServiceInterface getTicketServiceInterface() {
        return null;
    }

    public static void main(String[] args) {
        TicketShopRest ticketShopRest = new TicketShopRest();
        CLIClient cliclient = new CLIClient(ticketShopRest);
        cliclient.start();
    }
}
