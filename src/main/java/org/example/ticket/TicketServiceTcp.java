package org.example.ticket;

import org.example.client.TcpClient;

import java.time.LocalDateTime;
import java.util.List;

public class TicketServiceTcp implements TicketServiceInterface {
    private TcpClient client;

    public TicketServiceTcp(TcpClient client) {
        this.client = client;
    }

    @Override
    public Ticket add(LocalDateTime purchaseDate, Long customerId, Long eventId) {
        client.connect();
        client.sendMessage(String.join(";", "ct", purchaseDate.toString(), customerId.toString(), eventId.toString()));
        Ticket ticket = (Ticket) client.receiveObject();
        client.disconnect();
        return ticket;
    }

    @Override
    public Ticket get(long id) {
        return null;
    }

    @Override
    public void delete(long id) {

    }

    @Override
    public List<Ticket> getAll() {
        client.connect();
        client.sendMessage("gat");
        List<Ticket> tickets = (List<Ticket>) client.receiveObject();
        client.disconnect();
        return tickets;
    }

    @Override
    public void deleteAll() {

    }
}
