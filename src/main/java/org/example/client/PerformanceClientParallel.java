package org.example.client;

import org.example.TicketShop;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;
import org.example.utils.LogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public class PerformanceClientParallel {
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;

    public PerformanceClientParallel(TicketShop ticketShop) {
        this.eventService = ticketShop.getEventServiceInterface();
        this.customerService = ticketShop.getCustomerServiceInterface();
        this.ticketService = ticketShop.getTicketServiceInterface();
    }

    public void createEvents(int nmbOfEvents, int nmbOfTickets) {
        long startTime = System.currentTimeMillis();
        IntStream.range(0, nmbOfEvents).parallel().forEach(i -> {
            LogService.log("Create-Event");
            eventService.add("Event" + i, "Uni", LocalDateTime.now().plusDays(1), nmbOfTickets);
        });
        System.out.println("Creating " + nmbOfEvents + " Events took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void createCustomers(int nmbOfCustomers) {
        long startTime = System.currentTimeMillis();
        IntStream.range(0, nmbOfCustomers).parallel().forEach(i -> {
            LogService.log("Create-Customer");
            customerService.add("Customer" + i, "customer" + i + "@email.de", LocalDateTime.now().minusYears(18));
        });
        System.out.println("Creating " + nmbOfCustomers + " Customers took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void buyTickets(int amount) {
        long startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        customerService.getAll().parallelStream().forEach(customer -> {
            eventService.getAll().parallelStream().forEach(event -> {
                if (event.getNmbTickets() > 0) {
                    IntStream.range(0, amount).parallel().forEach(i -> {
                        LogService.log("Create-Ticket");
                        ticketService.add(now, customer.getId(), event.getId());
                    });
                }
            });
        });
        System.out.println("Buying " + amount + " ticket for every customer for every event took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
