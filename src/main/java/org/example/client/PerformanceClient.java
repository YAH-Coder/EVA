package org.example.client;

import org.example.TicketShop;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;

import java.time.LocalDateTime;

public class PerformanceClient {
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;

    public PerformanceClient(TicketShop ticketShop) {
        this.eventService = ticketShop.getEventServiceInterface();
        this.customerService = ticketShop.getCustomerServiceInterface();
        this.ticketService = ticketShop.getTicketServiceInterface();
    }

    public void createEvents(int nmbOfEvents, int nmbOfTickets) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nmbOfEvents; i++) {
            eventService.add("Event" + i, "Uni", LocalDateTime.now().plusDays(1), nmbOfTickets);
        }
        System.out.println("Creating " + nmbOfEvents + " Events took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void createCustomers(int nmbOfCustomers) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nmbOfCustomers; i++) {
            customerService.add("Customer" + i, "customer" + i + "@email.de", LocalDateTime.now().minusYears(18));
        }
        System.out.println("Creating " + nmbOfCustomers + " Customers took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void buyTickets(int amount) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        for (Customer customer: customerService.getAll()) {
            for (Event event: eventService.getAll()) {
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
