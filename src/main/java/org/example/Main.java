package org.example;

public class Main {

    public static void main(String[] args) {
        TicketShop ticketShop = new TicketShop();
        Client client = new Client(ticketShop);
        client.start();
    }
}