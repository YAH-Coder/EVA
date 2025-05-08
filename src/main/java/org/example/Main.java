package org.example;

import org.example.client.CLIClient;
import org.example.client.PerformanceClient;

public class Main {

    public static void main(String[] args) {
        TicketShop ticketShop = new TicketShop();
//        CLIClient CLIClient = new CLIClient(ticketShop);
//        CLIClient.start();
        PerformanceClient performanceClient = new PerformanceClient(ticketShop);
        performanceClient.createEvents(100, 1000);
        performanceClient.createCustomers(1000);
        performanceClient.buyTickets();
    }
}