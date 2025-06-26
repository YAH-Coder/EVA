package org.example.spring;

import org.example.Server;
import org.example.TicketShopClient;
import org.example.TicketShopStringReader;
import org.example.client.PerformanceClient;
import org.example.event.EventServiceInterface;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    private static TicketShopStringReader ticketShopStringReader;

    @Bean
    public EventServiceInterface eventServiceInterface() {
        return ticketShopStringReader.getEventServiceInterface();
    }

    public static void main(String[] args) {
        ticketShopStringReader = new TicketShopStringReader();
        Server server = new Server(9090, ticketShopStringReader);
        server.start();
        TicketShopClient ticketShopClient = new TicketShopClient(9090);
        long start = System.currentTimeMillis();
        PerformanceClient performanceClient = new PerformanceClient(ticketShopClient);
        performanceClient.createEvents(100, 1000);
        performanceClient.createCustomers(10);
        performanceClient.buyTickets(1);
        performanceClient.createEvents(100, 2000);
        performanceClient.buyTickets(2);
        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + "ms");
        server.stop();
        SpringApplication.run(Application.class, args);
    }
}
