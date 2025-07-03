package org.example.spring;

import org.example.Server;
import org.example.TicketShopStringReader;
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
        SpringApplication.run(Application.class, args);
    }
}
