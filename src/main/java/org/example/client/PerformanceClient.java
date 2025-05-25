package org.example.client;

import org.example.TicketShop;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.CountDownLatch;

/**
 * Client for performing load and performance tests on the TicketShop application.
 * This client simulates multiple users creating events, customers, and buying tickets.
 */
public class PerformanceClient {
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;
    private final ExecutorService clientTaskExecutor;
    private final int numClientThreads;

    /**
     * Constructs a new PerformanceClient.
     * Initializes services and a thread pool for concurrent task execution.
     *
     * @param ticketShop The TicketShop instance to interact with.
     */
    public PerformanceClient(TicketShop ticketShop) {
        this.eventService = ticketShop.getEventServiceInterface();
        this.customerService = ticketShop.getCustomerServiceInterface();
        this.ticketService = ticketShop.getTicketServiceInterface();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        this.numClientThreads = Math.max(1, availableProcessors / 8);

        ThreadFactory clientThreadFactory = new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                t.setName("PerformanceClient-Worker-" + counter++);
                return t;
            }
        };
        this.clientTaskExecutor = Executors.newFixedThreadPool(this.numClientThreads, clientThreadFactory);
        
        System.out.println("PerformanceClient initialized with " + this.numClientThreads + " worker threads.");
    }

    /**
     * Creates a specified number of events concurrently.
     * Each event creation is submitted as a task to the client's thread pool.
     *
     * @param nmbOfEvents The number of events to create.
     * @param nmbOfTickets The number of tickets available for each event.
     * @throws InterruptedException If the current thread is interrupted while waiting for tasks to complete.
     */
    public void createEvents(int nmbOfEvents, int nmbOfTickets) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(nmbOfEvents);

        for (int i = 0; i < nmbOfEvents; i++) {
            final int eventIndex = i;
            clientTaskExecutor.submit(() -> {
                StatisticsService.getInstance().recordTaskExecution("CreateEventTask", "PerformanceClientExecutor", Thread.currentThread().getName());
                try {
                    eventService.add("Event" + eventIndex, "Uni", LocalDateTime.now().plusDays(1), nmbOfTickets);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Event creation task for Event" + eventIndex + " was interrupted: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Exception during creation of Event" + eventIndex + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for event creation tasks to complete: " + e.getMessage());
        }
        
        System.out.println("Parallel creation of " + nmbOfEvents + " Events took " + (System.currentTimeMillis() - startTime) + "ms using " + this.numClientThreads + " threads.");
    }

    /**
     * Creates a specified number of customers sequentially.
     *
     * @param nmbOfCustomers The number of customers to create.
     * @throws InterruptedException If the underlying customer service operation is interrupted.
     */
    public void createCustomers(int nmbOfCustomers) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nmbOfCustomers; i++) {
            customerService.add("Customer" + i, "customer" + i + "@email.de", LocalDateTime.now().minusYears(18));
        }
        System.out.println("Creating " + nmbOfCustomers + " Customers took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * Simulates buying a specified number of tickets for every customer for every available event.
     * This method iterates through all customers and all events, attempting to buy tickets.
     *
     * @param amount The number of tickets each customer attempts to buy per event.
     * @throws InterruptedException If the underlying service operation is interrupted.
     */
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
