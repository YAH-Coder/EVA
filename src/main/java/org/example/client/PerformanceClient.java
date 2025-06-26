package org.example.client;

import org.example.TicketShopInterface;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PerformanceClient {
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;
    private final List<Event> newlyCreatedEvents = new ArrayList<>();

    public PerformanceClient(TicketShopInterface ticketShopInterface) {
        this.eventService = ticketShopInterface.getEventServiceInterface();
        this.customerService = ticketShopInterface.getCustomerServiceInterface();
        this.ticketService = ticketShopInterface.getTicketServiceInterface();
    }

    public void createEvents(int nmbOfEvents, int nmbOfTickets) {
        long startTime = System.currentTimeMillis();
        newlyCreatedEvents.clear();

        for (int i = 0; i < nmbOfEvents; i++) {
            Event event = eventService.add("Event" + i, "Uni", LocalDateTime.now().plusDays(1), nmbOfTickets);
            newlyCreatedEvents.add(event);
        }
        System.out.println("Creating " + nmbOfEvents + " Events took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void createCustomers(int nmbOfCustomers) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nmbOfCustomers; i++) {
            customerService.add("Customer" + i, "customer" + i + "@email.de", LocalDateTime.now().minusYears(18));
        }
        System.out.println("Creating " + nmbOfCustomers + " Customers took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void buyTickets(int amount) {
        long startTime = System.currentTimeMillis();
        for (Customer customer: customerService.getAll()) {
            for (Event event: newlyCreatedEvents) {
                if (event.getNmbTickets() == 0) {
                    continue;
                }
                for (int i = 0; i < amount; i++) {
                    ticketService.add(LocalDateTime.now(), customer.getId(), event.getId());
                }
            }
        }
        System.out.println("Buying " + amount + " ticket for every customer for every event took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}