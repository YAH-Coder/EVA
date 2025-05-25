package org.example;

import org.example.client.PerformanceClient;
import org.example.utils.StatisticsService;

/**
 * Main class for the TicketShop application.
 * This class initializes the application and runs performance tests.
 */
public class Main {

    /**
     * Main method to start the application.
     * Initializes the TicketShop and runs performance tests.
     *
     * @param args Command line arguments (not used).
     * @throws InterruptedException If any thread is interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        TicketShop ticketShop = new TicketShop();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("-----------------------------------");
            System.out.println("Application Shutdown Statistics:");
            System.out.println(StatisticsService.getInstance().getStatistics());
            System.out.println("-----------------------------------");
        }));

        PerformanceClient performanceClient = new PerformanceClient(ticketShop);
        performanceClient.createEvents(100, 1000);
        performanceClient.createCustomers(1000);
        performanceClient.buyTickets(1);
        performanceClient.createEvents(100, 2000);
        performanceClient.buyTickets(2);
        System.out.println("Total time " + (System.currentTimeMillis() - startTime) + "ms");
    }
}