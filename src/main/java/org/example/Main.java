package org.example;

import org.example.client.PerformanceClient;
import org.example.client.PerformanceClientParallel;

public class Main {

    public static void main(String[] args) {
        TicketShop ticketShop = new TicketShop();
//        CLIClient CLIClient = new CLIClient(ticketShop);
//        CLIClient.start();
//        long start = System.currentTimeMillis();
//        PerformanceClient performanceClient = new PerformanceClient(ticketShop);
//        performanceClient.createEvents(100, 1000);
//        performanceClient.createCustomers(1000);
//        performanceClient.buyTickets(1);
//        performanceClient.createEvents(100, 2000);
//        performanceClient.buyTickets(2);
//        long end = System.currentTimeMillis();
//        System.out.println("Total time: " + (end - start) + "ms");

        long start = System.currentTimeMillis();
        PerformanceClientParallel performanceClientParallel = new PerformanceClientParallel(ticketShop);
        performanceClientParallel.createEvents(100, 1000);
        performanceClientParallel.createCustomers(1000);
        performanceClientParallel.buyTickets(1);
        performanceClientParallel.createEvents(100, 2000);
        performanceClientParallel.buyTickets(2);
        long endParallel = System.currentTimeMillis();
        System.out.println("Total time parallel: " + (endParallel - start) + "ms");

//        long start = System.currentTimeMillis();
//        PerformanceClientParallelVirtualThreads performanceClientParallelVirtualThreads = new PerformanceClientParallelVirtualThreads(ticketShop);
//        performanceClientParallelVirtualThreads.createEvents(100, 1000);
//        performanceClientParallelVirtualThreads.createCustomers(1000);
//        performanceClientParallelVirtualThreads.buyTickets(1);
//        performanceClientParallelVirtualThreads.createEvents(100, 2000);
//        performanceClientParallelVirtualThreads.buyTickets(2);
//        long endParallelVirtualThreads = System.currentTimeMillis();
//        System.out.println("Total time parallel virtual threads: " + (endParallelVirtualThreads - start) + "ms");
    }
}