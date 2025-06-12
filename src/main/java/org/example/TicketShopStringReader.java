package org.example;

import org.example.customer.CustomerService;
import org.example.customer.CustomerServiceInterface;
import org.example.event.EventService;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketService;
import org.example.ticket.TicketServiceInterface;

import java.time.LocalDateTime;

public class TicketShopStringReader {
    private final CustomerServiceInterface customerServiceInterface;
    private final EventServiceInterface eventServiceInterface;
    private final TicketServiceInterface ticketServiceInterface;

    public TicketShopStringReader() {
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

    public String execute(String command) {
        String[] parts = command.split(";");
        if (parts.length == 0) {
            return "No command provided.";
        }
        switch( parts[0] ) {
            case "ce":
                if (parts.length != 5) {
                    return "Invalid command format for creating event.";
                }
                try {
                    String name = parts[1];
                    String location = parts[2];
                    LocalDateTime date = LocalDateTime.parse(parts[3]);
                    int nmbTickets = Integer.parseInt(parts[4]);
                    eventServiceInterface.add(name, location, date, nmbTickets);
                   return "Event created: " + name;
                } catch (Exception e) {
                    return "Error creating event: " + e.getMessage();
                }
            default:
                return "Unknown command: " + command;
        }
    }
}
