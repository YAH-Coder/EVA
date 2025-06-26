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

    public Object execute(String command) {
        String[] parts = command.split(";");
        if (parts.length == 0) {
            return "No command provided.";
        }
        switch( parts[0] ) {
            case "ce":
                if (parts.length != 5) {
                    return "Invalid command format";
                }
                try {
                    String name = parts[1];
                    String location = parts[2];
                    LocalDateTime date = LocalDateTime.parse(parts[3]);
                    int nmbTickets = Integer.parseInt(parts[4]);
                    return eventServiceInterface.add(name, location, date, nmbTickets);
                } catch (Exception e) {
                    return "Error creating event: " + e.getMessage();
                }
            case "gae":
                if (parts.length != 1) {
                    return "Invalid command format";
                }
                try {
                    return eventServiceInterface.getAll();
                } catch (Exception e) {
                    return "Error getting all events: " + e.getMessage();
                }
            case "cc":
                if (parts.length != 4) {
                    return "Invalid command format";
                }
                try {
                    String username = parts[1];
                    String email = parts[2];
                    LocalDateTime birthday = LocalDateTime.parse(parts[3]);
                    return customerServiceInterface.add(username, email, birthday);
                } catch (Exception e) {
                    return "Error creating user: " + e.getMessage();
                }
            case "gac":
                if (parts.length != 1) {
                    return "Invalid command format";
                }
                try {
                    return customerServiceInterface.getAll();
                } catch (Exception e) {
                    return "Error getting all customers: " + e.getMessage();
                }
            case "ct":
                if (parts.length != 4) {
                    return "Invalid command format";
                }
                try {
                    LocalDateTime purchaseDate= LocalDateTime.parse(parts[1]);
                    Long customerId = Long.parseLong(parts[2]);
                    Long eventId = Long.parseLong(parts[3]);
                    return ticketServiceInterface.add(purchaseDate, customerId, eventId);
                } catch (Exception e) {
                    return "Error creating ticket: " + e.getMessage();
                }
            case "gat":
                if (parts.length != 1) {
                    return "Invalid command format";
                }
                try {
                    return ticketServiceInterface.getAll();
                } catch (Exception e) {
                    return "Error getting all tickets: " + e.getMessage();
                }
            default:
                return "Unknown command: " + command;
        }
    }
}
