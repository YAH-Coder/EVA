package org.example.event;

import org.example.client.TcpClient;

import java.time.LocalDateTime;
import java.util.List;

public class EventServiceTcp implements EventServiceInterface {
    private TcpClient client;

    public EventServiceTcp(TcpClient client) {
        this.client = client;
    }

    @Override
    public Event add(String name, String location, LocalDateTime date, int nmbTickets) {
        client.connect();
        client.sendMessage(String.join(";", "ce", name, location, date.toString(), Integer.toString(nmbTickets)));
        System.out.println(client.receiveMessage());
        client.disconnect();
        return null;
    }

    @Override
    public Event get(long id) {
        return null;
    }

    @Override
    public void update(long id, String name, String location, LocalDateTime date, int nmbTickets) {
    }

    @Override
    public void delete(long id) {
    }

    @Override
    public List<Event> getAll() {
        return null;
    }

    @Override
    public void deleteAll() {
    }
}
