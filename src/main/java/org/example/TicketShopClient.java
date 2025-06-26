package org.example;

import org.example.customer.CustomerService;
import org.example.customer.CustomerServiceInterface;
import org.example.customer.CustomerServiceTcp;
import org.example.event.EventServiceInterface;
import org.example.event.EventServiceTcp;
import org.example.ticket.TicketService;
import org.example.ticket.TicketServiceInterface;
import org.example.client.TcpClient;
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
}