package org.example;

import org.example.client.PerformanceClient;
import org.example.client.TcpClient;
import org.example.customer.CustomerServiceInterface;
import org.example.customer.CustomerServiceTcp;
import org.example.event.EventServiceInterface;
import org.example.event.EventServiceTcp;
import org.example.ticket.TicketServiceInterface;
import org.example.ticket.TicketServiceTcp;

public class TicketShopClient implements TicketShopInterface {
    private final CustomerServiceInterface customerServiceInterface;
    private final EventServiceInterface eventServiceInterface;
    private final TicketServiceInterface ticketServiceInterface;
    private final TcpClient tcpClient;

    public TicketShopClient(String host, int port) {
        this.tcpClient = new TcpClient(host, port);
        this.customerServiceInterface = new CustomerServiceTcp(tcpClient);
        this.eventServiceInterface = new EventServiceTcp(tcpClient);
        this.ticketServiceInterface = new TicketServiceTcp(tcpClient);
    }

    public TicketShopClient(int port) {
        this("localhost", port);
    }

    @Override
    public CustomerServiceInterface getCustomerServiceInterface() {
        return customerServiceInterface;
    }

    @Override
    public EventServiceInterface getEventServiceInterface() {
        return eventServiceInterface;
    }

    @Override
    public TicketServiceInterface getTicketServiceInterface() {
        return ticketServiceInterface;
    }

    public static void main(String[] args) {
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
    }
}