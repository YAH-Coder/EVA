package org.example.client;

import org.example.TicketShopInterface;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.TicketServiceInterface;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerformanceClientParallelVirtualThreads {
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;

    public PerformanceClientParallelVirtualThreads(TicketShopInterface ticketShopInterface) {
        this.eventService = ticketShopInterface.getEventServiceInterface();
        this.customerService = ticketShopInterface.getCustomerServiceInterface();
        this.ticketService = ticketShopInterface.getTicketServiceInterface();
    }

    public void createEvents(int nmbOfEvents, int nmbOfTickets) {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(nmbOfEvents);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < nmbOfEvents; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        eventService.add("Event" + index, "Uni", LocalDateTime.now().plusDays(1), nmbOfTickets);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Creating " + nmbOfEvents + " Events took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void createCustomers(int nmbOfCustomers) {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(nmbOfCustomers);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < nmbOfCustomers; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        customerService.add("Customer" + index, "customer" + index + "@email.de", LocalDateTime.now().minusYears(18));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Creating " + nmbOfCustomers + " Customers took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void buyTickets(int amount) {
        long startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        List<Customer> customers = customerService.getAll();
        List<Event> events = eventService.getAll();

        // Count the total number of tasks we'll create
        int totalTasks = 0;
        for (Event event : events) {
            if (event.getNmbTickets() > 0) {
                totalTasks += customers.size() * amount;
            }
        }

        CountDownLatch latch = new CountDownLatch(totalTasks);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Customer customer : customers) {
                for (Event event : events) {
                    if (event.getNmbTickets() > 0) {
                        for (int i = 0; i < amount; i++) {
                            executor.submit(() -> {
                                try {
                                    ticketService.add(now, customer.getId(), event.getId());
                                } finally {
                                    latch.countDown();
                                }
                            });
                        }
                    }
                }
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Buying " + amount + " ticket for every customer for every event took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}