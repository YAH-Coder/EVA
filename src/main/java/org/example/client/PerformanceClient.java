package org.example.client;

import org.example.TicketShop;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService; // Added import
import java.util.concurrent.Executors;     // Added import
import java.util.concurrent.ThreadFactory;  // Added import

public class PerformanceClient {
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;
    private final ExecutorService clientTaskExecutor; // Added field
    private final int numClientThreads;             // Added field

    public PerformanceClient(TicketShop ticketShop) {
        this.eventService = ticketShop.getEventServiceInterface();
        this.customerService = ticketShop.getCustomerServiceInterface();
        this.ticketService = ticketShop.getTicketServiceInterface();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        this.numClientThreads = Math.max(1, availableProcessors / 8); // Use approx 1/8th of cores, min 1

        ThreadFactory clientThreadFactory = new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true); // Client tasks can be daemon threads
                t.setName("PerformanceClient-Worker-" + counter++);
                return t;
            }
        };
        this.clientTaskExecutor = Executors.newFixedThreadPool(this.numClientThreads, clientThreadFactory);
        
        // Log the number of threads being used by PerformanceClient
        System.out.println("PerformanceClient initialized with " + this.numClientThreads + " worker threads.");
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
