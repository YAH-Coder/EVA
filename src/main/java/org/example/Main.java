package org.example;

import org.example.client.CLIClient;
import org.example.client.PerformanceClient;
// import org.example.utils.IDServiceParallel; // Removed

public class Main {

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        // IDServiceParallel idService = new IDServiceParallel(10000); // Removed
        TicketShop ticketShop = new TicketShop(); // Changed constructor
//        CLIClient CLIClient = new CLIClient(ticketShop);
//        CLIClient.start();
        PerformanceClient performanceClient = new PerformanceClient(ticketShop);
        performanceClient.createEvents(100, 1000);
        performanceClient.createCustomers(1000);
        performanceClient.buyTickets(1);
        performanceClient.createEvents(100, 2000);
        performanceClient.buyTickets(2);
        System.out.println("Total time " + (System.currentTimeMillis() - startTime) + "ms");
    }
}